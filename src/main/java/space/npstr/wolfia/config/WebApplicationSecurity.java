/*
 * Copyright (C) 2016-2019 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.wolfia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import space.npstr.wolfia.config.properties.WolfiaConfig;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class WebApplicationSecurity extends WebSecurityConfigurerAdapter {

    public static final GrantedAuthority USER = new SimpleGrantedAuthority("USER");
    public static final GrantedAuthority OWNER = new SimpleGrantedAuthority("OWNER");

    private static final Logger log = LoggerFactory.getLogger(WebApplicationSecurity.class);

    private static final String[] MACHINE_ENDPOINTS = {"/metrics"};
    private static final String[] PUBLIC_ENDPOINTS = {"/api/oauth2/**"};

    private final WolfiaConfig config;

    public WebApplicationSecurity(WolfiaConfig config) {
        this.config = config;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        String[] noAuthEndpoints = Stream.concat(Arrays.stream(MACHINE_ENDPOINTS), Arrays.stream(PUBLIC_ENDPOINTS))
                .collect(Collectors.toSet())
                .toArray(new String[]{});

        http
                .csrf().ignoringAntMatchers(MACHINE_ENDPOINTS)
                .and().authorizeRequests()
                .antMatchers(noAuthEndpoints).permitAll()
                .anyRequest().authenticated()
                .and().formLogin()
                .and().httpBasic()
        ;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        var inMemory = auth.inMemoryAuthentication();
        String webAdmin = this.config.getWebAdmin();
        String webPass = this.config.getWebPass();
        if (StringUtils.isEmpty(webAdmin) || StringUtils.isEmpty(webPass)) {
            log.warn("Web admin/pass is empty, so any dashboards, etc. will not be accessible.");
            return;
        }

        inMemory
                .withUser(webAdmin)
                .password(passwordEncoder().encode(webPass))
                .roles(USER.getAuthority(), OWNER.getAuthority());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
