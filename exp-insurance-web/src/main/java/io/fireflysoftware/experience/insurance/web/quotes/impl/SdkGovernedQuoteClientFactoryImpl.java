/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.experience.insurance.web.quotes.impl;

import io.fireflysoftware.domain.insurance.underwriting.sdk.api.GovernedQuoteOffersApi;
import io.fireflysoftware.domain.insurance.underwriting.sdk.invoker.ApiClient;
import io.fireflysoftware.experience.insurance.core.journey.quotes.GovernedQuoteClientFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

public final class SdkGovernedQuoteClientFactoryImpl implements GovernedQuoteClientFactory {
    private final String basePath;
    private final String audience;
    private final boolean loopback;
    private final WebClient transport = ApiClient.buildWebClientBuilder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(1024 * 1024)).build();

    public SdkGovernedQuoteClientFactoryImpl(String basePath, String audience, boolean loopback) {
        this.basePath = basePath;
        this.audience = audience;
        this.loopback = loopback;
    }

    @Override
    public Mono<GovernedQuoteOffersApi> current() {
        return Mono.defer(() -> {
            if (!validEndpoint(basePath, loopback) || audience == null || audience.isBlank()) {
                return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CONFIGURED_QUOTE_DOMAIN_REQUIRED"));
            }
            return ReactiveSecurityContextHolder.getContext()
                    .flatMap(context -> Mono.justOrEmpty(context.getAuthentication()))
                    .filter(authentication -> authentication.isAuthenticated()
                            && authentication instanceof JwtAuthenticationToken)
                    .cast(JwtAuthenticationToken.class)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "VERIFIED_INSURANCE_CALLER_REQUIRED")))
                    .map(authentication -> {
                        var jwt = authentication.getToken();
                        if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) {
                            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "CURRENT_INSURANCE_CALLER_REQUIRED");
                        }
                        if (!jwt.getAudience().contains(audience)) {
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "INSURANCE_DOMAIN_AUDIENCE_REQUIRED");
                        }
                        var client = new ApiClient(transport).setBasePath(basePath);
                        client.setBearerToken(jwt.getTokenValue());
                        return new GovernedQuoteOffersApi(client);
                    });
        });
    }

    public static boolean validEndpoint(String value, boolean loopback) {
        try {
            var uri = URI.create(value);
            return uri.getHost() != null && uri.getRawUserInfo() == null && uri.getRawQuery() == null
                    && uri.getRawFragment() == null && ("https".equals(uri.getScheme())
                    || loopback && "http".equals(uri.getScheme())
                    && Set.of("127.0.0.1", "localhost", "::1", "[::1]").contains(uri.getHost()));
        } catch (RuntimeException invalid) {
            return false;
        }
    }
}
