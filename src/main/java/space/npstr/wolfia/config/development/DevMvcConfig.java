/*
 * Copyright (C) 2016-2020 Dennis Neufeld
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

package space.npstr.wolfia.config.development;

import java.util.Optional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import space.npstr.wolfia.SpringProfiles;
import space.npstr.wolfia.webapi.LoginRedirect;

@Profile(SpringProfiles.FAKE_LOGIN)
@Configuration
public class DevMvcConfig implements WebMvcConfigurer {

    private final Optional<FakeAuther> fakeAuther;

    public DevMvcConfig(Optional<FakeAuther> fakeAuther) {
        this.fakeAuther = fakeAuther;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        fakeAuther.ifPresent(f -> registry.addInterceptor(f).addPathPatterns(LoginRedirect.ROUTE));
    }
}
