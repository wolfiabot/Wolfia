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

package space.npstr.wolfia.domain.staff;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.CheckReturnValue;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import space.npstr.wolfia.App;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.db.gen.enums.StaffFunction;
import space.npstr.wolfia.db.gen.tables.records.StaffMemberRecord;
import space.npstr.wolfia.domain.UserCache;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;

import static java.util.Optional.ofNullable;

/**
 * Provides and updates information about the staff behind Wolfia
 */
@Service
public class StaffService {

    private static final Logger log = LoggerFactory.getLogger(StaffService.class);

    private final StaffRepository staffRepository;
    private final UserCache userCache;
    private final ShardManager shardManager;
    private final WolfiaConfig wolfiaConfig;

    public StaffService(StaffRepository staffRepository, UserCache userCache, ShardManager shardManager,
                        WolfiaConfig wolfiaConfig, ExceptionLoggingExecutor scheduler) {
        this.staffRepository = staffRepository;
        this.userCache = userCache;
        this.shardManager = shardManager;
        this.wolfiaConfig = wolfiaConfig;

        scheduler.scheduleAtFixedRate(this::updateIfPossible, 10, 60, TimeUnit.SECONDS);
    }

    /**
     * @return a list of all enabled and active staff members
     */
    public List<StaffMember> getEnabledActiveStaffMembers() {
        return this.staffRepository.fetchAllStaffMembers()
                .toCompletableFuture().join().stream()
                .filter(StaffMemberRecord::getActive)
                .filter(this::isEnabled)
                .map(this::toStaffMember)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private boolean isEnabled(StaffMemberRecord record) {
        return
//                wolfiaConfig.isDebug() || //for debugging/developing, it is handy to see a large list of staffers
                record.getEnabled();
    }

    /**
     * Entry method to further fluent operations on specific users.
     */
    @CheckReturnValue
    public Action user(long userId) {
        return new Action(userId, this.staffRepository, this);
    }

    public static class Action {
        private final long userId;
        private final StaffRepository staffRepository;
        private final StaffService staffService;

        private Action(long userId, StaffRepository staffRepository, StaffService staffService) {
            this.userId = userId;
            this.staffRepository = staffRepository;
            this.staffService = staffService;
        }

        Optional<StaffMember> get() {
            return this.staffRepository.getStaffMember(userId)
                    .toCompletableFuture().join()
                    .flatMap(this.staffService::toStaffMember);
        }

        StaffMember enable() {
            return this.staffRepository.updateEnabled(userId, true)
                    .toCompletableFuture().join()
                    .flatMap(this.staffService::toStaffMember)
                    .orElseThrow();
        }

        StaffMember disable() {
            return this.staffRepository.updateEnabled(userId, false)
                    .toCompletableFuture().join()
                    .flatMap(this.staffService::toStaffMember)
                    .orElseThrow();
        }

        StaffMember setSlogan(String slogan) {
            return this.staffRepository.updateSlogan(userId, slogan)
                    .toCompletableFuture().join()
                    .flatMap(this.staffService::toStaffMember)
                    .orElseThrow();
        }

        StaffMember removeSlogan() {
            return this.staffRepository.updateSlogan(userId, null)
                    .toCompletableFuture().join()
                    .flatMap(this.staffService::toStaffMember)
                    .orElseThrow();
        }

        StaffMember setLink(URI uri) {
            return this.staffRepository.updateLink(userId, uri)
                    .toCompletableFuture().join()
                    .flatMap(this.staffService::toStaffMember)
                    .orElseThrow();
        }

        StaffMember removeLink() {
            return this.staffRepository.updateLink(userId, null)
                    .toCompletableFuture().join()
                    .flatMap(this.staffService::toStaffMember)
                    .orElseThrow();
        }
    }

    /**
     * Attempts to update the staff member records if the wolfia lounge is available.
     * This task is also run periodically.
     */
    public void updateIfPossible() {
        Guild wolfiaLounge = shardManager.getGuildById(App.WOLFIA_LOUNGE_ID);
        if (wolfiaLounge != null) {
            updateStaff(wolfiaLounge).toCompletableFuture().join();
        }
    }

    @CheckReturnValue
    private CompletionStage<Void> updateStaff(Guild wolfiaLounge) {
        List<Member> moderators = findHumanMembersOfRole(wolfiaLounge, App.MODERATOR_ROLE_ID);
        List<Member> setupManagers = findHumanMembersOfRole(wolfiaLounge, App.SETUP_MANAGER_ROLE_ID);
        List<Member> developers = findHumanMembersOfRole(wolfiaLounge, App.DEVELOPER_ROLE_ID);

        var updates = new ArrayList<CompletionStage<StaffMemberRecord>>();
        for (var moderator : moderators) {
            var update = this.staffRepository.updateOrCreateStaffMemberFunction(moderator.getIdLong(), StaffFunction.MODERATOR);
            updates.add(update);
        }
        for (var setupManager : setupManagers) {
            var update = this.staffRepository.updateOrCreateStaffMemberFunction(setupManager.getIdLong(), StaffFunction.SETUP_MANAGER);
            updates.add(update);
        }
        for (var developer : developers) {
            var update = this.staffRepository.updateOrCreateStaffMemberFunction(developer.getIdLong(), StaffFunction.DEVELOPER);
            updates.add(update);
        }

        List<Member> staff = new ArrayList<>();
        staff.addAll(moderators);
        staff.addAll(setupManagers);
        staff.addAll(developers);
        Set<Long> staffIds = staff.stream().map(ISnowflake::getIdLong).collect(Collectors.toSet());

        //noinspection SuspiciousToArrayCall
        return CompletableFuture.allOf(updates.toArray(new CompletableFuture[]{}))
                .thenCompose(__ -> this.staffRepository.updateAllActive(staffIds));
    }

    private List<Member> findHumanMembersOfRole(Guild wolfiaLounge, long roleId) {
        Role role = wolfiaLounge.getRoleById(roleId);
        if (role == null) {
            log.warn("Could not find role {} in the wolfia lounge, where did it go?", roleId);
            return List.of();
        }
        return wolfiaLounge.getMembersWithRoles(role).stream()
                .filter(member -> !member.getUser().isBot())
                .collect(Collectors.toList());
    }

    private Optional<StaffMember> toStaffMember(StaffMemberRecord staffMemberRecord) {
        Optional<User> userOpt = userCache.user(staffMemberRecord.getUserId()).get();
        return userOpt.map(user -> ImmutableStaffMember.builder()
                .discordId(user.getIdLong())
                .name(user.getName())
                .discriminator(user.getDiscriminator())
                .function(staffMemberRecord.getFunction())
                .isEnabled(staffMemberRecord.getEnabled())
                .isActive(staffMemberRecord.getActive())
                .avatarId(ofNullable(user.getAvatarId()))
                .slogan(ofNullable(staffMemberRecord.getSlogan()))
                .link(ofNullable(staffMemberRecord.getLink()))
                .build());
    }

}
