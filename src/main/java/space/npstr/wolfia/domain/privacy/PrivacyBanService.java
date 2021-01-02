/*
 * Copyright (C) 2016-2020 the original author or authors
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

package space.npstr.wolfia.domain.privacy;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;

import static space.npstr.wolfia.App.WOLFIA_LOUNGE_ID;

@Service
public class PrivacyBanService {

    private static final Logger log = LoggerFactory.getLogger(PrivacyBanService.class);

    private final ShardManager shardManager;
    private final PrivacyRepository privacyRepository;

    public PrivacyBanService(ShardManager shardManager, PrivacyRepository privacyRepository,
                             ExceptionLoggingExecutor executor) {

        this.shardManager = shardManager;
        this.privacyRepository = privacyRepository;

        executor.scheduleAtFixedRate(this::syncBans, 10, 10, TimeUnit.MINUTES);
    }

    @EventListener
    public void onDataDelete(PersonalDataDelete dataDelete) {
        privacyBanAll(List.of(dataDelete.userId()));
    }

    public void syncBans() {
        List<Long> allDenied = privacyRepository.findAllDeniedProcessData()
                .toCompletableFuture().join()
                .stream()
                .map(Privacy::getUserId)
                .collect(Collectors.toList());

        privacyBanAll(allDenied);
    }

    public void privacyBanAll(List<Long> userIds) {
        Guild homeGuild = shardManager.getGuildById(WOLFIA_LOUNGE_ID);
        if (homeGuild == null) {
            return; //we will pick it up on the next sync run
        }

        homeGuild.retrieveBanList().submit().thenAccept(banList -> {
            for (long userId : userIds) {
                boolean isBanned = banList.stream().anyMatch(ban -> ban.getUser().getIdLong() == userId);
                if (isBanned) {
                    return; // nothing to do here
                }

                homeGuild.ban(Long.toString(userId), 0, "Privacy: Data Processing Denied")
                        .submit()
                        .whenComplete((aVoid, throwable) -> {
                            if (throwable != null) {
                                log.error("Failed to ban user {}", userId, throwable);
                            }
                        });
            }
        }).whenComplete((aVoid, throwable) -> {
            if (throwable != null) {
                log.error("Failed to sync bans", throwable);
            }
        });
    }
}
