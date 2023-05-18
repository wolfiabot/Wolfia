/*
 * Copyright (C) 2016-2023 the original author or authors
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
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.App;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.db.gen.enums.StaffFunction;
import space.npstr.wolfia.domain.UserCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static space.npstr.wolfia.TestUtil.uniqueLong;
import static space.npstr.wolfia.db.gen.Tables.STAFF_MEMBER;

class StaffServiceTest extends ApplicationTest {

    @Autowired
    private Database database;

    @Autowired
    private StaffService staffService;

    @Autowired
    protected ShardManager shardManager;

    private long developerUserId = uniqueLong();
    private long moderatorUserId = uniqueLong();
    private long setupManagerUserId = uniqueLong();
    private long botUserId = uniqueLong();
    private Guild guild;
    private Role developerRole;

    @BeforeEach
    void setup() {
        this.database.jooq().transactionResult(config -> config.dsl()
                .deleteFrom(STAFF_MEMBER)
                .execute()
        );
        guild = mock(Guild.class);
        developerRole = mock(Role.class);
        Role moderatorRole = mock(Role.class);
        Role setupManagerRole = mock(Role.class);
        when(guild.getRoleById(eq(App.DEVELOPER_ROLE_ID))).thenReturn(developerRole);
        when(guild.getRoleById(eq(App.MODERATOR_ROLE_ID))).thenReturn(moderatorRole);
        when(guild.getRoleById(eq(App.SETUP_MANAGER_ROLE_ID))).thenReturn(setupManagerRole);

        Member developer = mockMember(this.developerUserId);
        Member moderator = mockMember(this.moderatorUserId);
        Member setupManager = mockMember(this.setupManagerUserId);
        Member bot = mockMemberBot(this.botUserId);

        when(guild.getMembersWithRoles(eq(developerRole))).thenReturn(List.of(developer, bot));
        when(guild.getMembersWithRoles(eq(moderatorRole))).thenReturn(List.of(moderator));
        when(guild.getMembersWithRoles(eq(setupManagerRole))).thenReturn(List.of(setupManager));

        UserCache.Action getDeveloperAction = mock(UserCache.Action.class);
        doReturn(Optional.of(developer.getUser())).when(getDeveloperAction).fetch();
        doReturn(getDeveloperAction).when(this.userCache).user(eq(this.developerUserId));

        UserCache.Action getModeratorAction = mock(UserCache.Action.class);
        doReturn(Optional.of(moderator.getUser())).when(getModeratorAction).fetch();
        doReturn(getModeratorAction).when(this.userCache).user(eq(this.moderatorUserId));

        UserCache.Action getSetupManagerAction = mock(UserCache.Action.class);
        doReturn(Optional.of(setupManager.getUser())).when(getSetupManagerAction).fetch();
        doReturn(getSetupManagerAction).when(this.userCache).user(eq(this.setupManagerUserId));

        UserCache.Action getBotAction = mock(UserCache.Action.class);
        doReturn(Optional.of(bot.getUser())).when(getBotAction).fetch();
        doReturn(getBotAction).when(this.userCache).user(eq(this.botUserId));

        doReturn(guild).when(this.shardManager).getGuildById(eq(App.WOLFIA_LOUNGE_ID));
    }

    private Member mockMember(long userId) {
        return mockMember(userId, false);
    }

    private Member mockMemberBot(long userId) {
        return mockMember(userId, true);
    }

    private Member mockMember(long userId, boolean isBot) {
        User user = mock(User.class);
        when(user.getIdLong()).thenReturn(userId);
        when(user.getName()).thenReturn("Foo");
        when(user.getDiscriminator()).thenReturn("0001");
        when(user.isBot()).thenReturn(isBot);

        Member member = mock(Member.class);
        when(member.getIdLong()).thenReturn(userId);
        when(member.getUser()).thenReturn(user);
        return member;
    }

    @Test
    void givenGuildAvailable_whenUpdateIfPossible_shouldUpdateStaff() {
        this.staffService.updateIfPossible();

        StaffMember developer = this.staffService.user(this.developerUserId).get();
        assertThat(developer).satisfies(isStaffMember(this.developerUserId, StaffFunction.DEVELOPER));
        StaffMember moderator = this.staffService.user(this.moderatorUserId).get();
        assertThat(moderator).satisfies(isStaffMember(this.moderatorUserId, StaffFunction.MODERATOR));
        StaffMember setupManager = this.staffService.user(this.setupManagerUserId).get();
        assertThat(setupManager).satisfies(isStaffMember(this.setupManagerUserId, StaffFunction.SETUP_MANAGER));
    }

    @Test
    void givenGuildNotAvailable_whenUpdateIfPossible_shouldDoNothing() {
        doReturn(null).when(this.shardManager).getGuildById(eq(App.WOLFIA_LOUNGE_ID));

        this.staffService.updateIfPossible();

        StaffMember developer = this.staffService.user(this.developerUserId).get();
        assertThat(developer).isNull();
        StaffMember moderator = this.staffService.user(this.moderatorUserId).get();
        assertThat(moderator).isNull();
        StaffMember setupManager = this.staffService.user(this.setupManagerUserId).get();
        assertThat(setupManager).isNull();
    }

    @Test
    void whenUpdateIfPossible_shouldIgnoreBotUsers() {
        this.staffService.updateIfPossible();

        StaffMember developer = this.staffService.user(this.botUserId).get();
        assertThat(developer).isNull();
    }

    @Test
    void givenUserLostStaffRole_whenUpdateIfPossible_shouldMakeStaffMemberInactive() {
        this.staffService.updateIfPossible();
        when(guild.getMembersWithRoles(eq(developerRole))).thenReturn(List.of());

        this.staffService.updateIfPossible();

        StaffMember developer = this.staffService.user(this.developerUserId).get();
        assertThat(developer).isNotNull();
        assertThat(developer.isActive()).isFalse();
    }

    @Test
    void whenGetEnabledActiveStaffMembers_shouldReturnEnabledActiveStaffMembers() {
        this.staffService.updateIfPossible();
        this.staffService.user(this.developerUserId).enable();
        this.staffService.user(this.moderatorUserId).enable();
        this.staffService.user(this.setupManagerUserId).enable();

        List<StaffMember> staffMembers = this.staffService.enabledActiveStaffMembers();

        assertThat(staffMembers).anySatisfy(isStaffMember(this.developerUserId, StaffFunction.DEVELOPER));
        assertThat(staffMembers).anySatisfy(isStaffMember(this.moderatorUserId, StaffFunction.MODERATOR));
        assertThat(staffMembers).anySatisfy(isStaffMember(this.setupManagerUserId, StaffFunction.SETUP_MANAGER));
    }

    @Test
    void whenGetEnabledActiveStaffMembers_shouldOnlyReturnActive() {
        this.staffService.updateIfPossible();
        when(guild.getMembersWithRoles(eq(developerRole))).thenReturn(List.of());
        this.staffService.updateIfPossible();
        this.staffService.user(this.developerUserId).enable();
        this.staffService.user(this.moderatorUserId).enable();
        this.staffService.user(this.setupManagerUserId).enable();

        List<StaffMember> staffMembers = this.staffService.enabledActiveStaffMembers();

        assertThat(staffMembers)
                .allMatch(StaffMember::isActive)
                .noneMatch(staffMember -> staffMember.getDiscordId() == developerUserId);
    }

    @Test
    void whenGetEnabledActiveStaffMembers_shouldOnlyReturnEnabled() {
        this.staffService.updateIfPossible();
        this.staffService.user(developerUserId).disable();
        this.staffService.user(this.moderatorUserId).enable();
        this.staffService.user(this.setupManagerUserId).enable();

        List<StaffMember> staffMembers = this.staffService.enabledActiveStaffMembers();

        assertThat(staffMembers)
                .allMatch(StaffMember::isEnabled)
                .noneMatch(staffMember -> staffMember.getDiscordId() == developerUserId);
    }

    @Test
    void givenExistingStaffMember_whenGetStaffMember_shouldReturnStaffMember() {
        this.staffService.updateIfPossible();

        StaffMember developer = this.staffService.user(this.developerUserId).get();
        assertThat(developer).satisfies(isStaffMember(this.developerUserId, StaffFunction.DEVELOPER));
        StaffMember moderator = this.staffService.user(this.moderatorUserId).get();
        assertThat(moderator).satisfies(isStaffMember(this.moderatorUserId, StaffFunction.MODERATOR));
        StaffMember setupManager = this.staffService.user(this.setupManagerUserId).get();
        assertThat(setupManager).satisfies(isStaffMember(this.setupManagerUserId, StaffFunction.SETUP_MANAGER));
    }

    private Consumer<StaffMember> isStaffMember(long userId, StaffFunction function) {
        return staff -> {
            assertThat(staff).isNotNull();
            assertThat(staff.getDiscordId()).isEqualTo(userId);
            assertThat(staff.getFunction()).isEqualTo(function);
        };
    }

    @Test
    void givenNoExistingStaffMember_whenGetStaffMember_shouldReturnEmpty() {
        this.staffService.updateIfPossible();

        StaffMember staff = this.staffService.user(uniqueLong()).get();

        assertThat(staff).isNull();
    }

    @Test
    void whenEnableStaffMember_shouldBeEnabled() {
        this.staffService.updateIfPossible();

        StaffMember returned = this.staffService.user(this.developerUserId).enable();

        StaffMember getted = this.staffService.user(this.developerUserId).get();
        assertThat(getted).isNotNull();
        assertThat(returned.isEnabled()).isTrue();
        assertThat(getted.isEnabled()).isTrue();
    }

    @Test
    void whenDisableStaffMember_shouldBeDisabled() {
        this.staffService.updateIfPossible();

        StaffMember returned = this.staffService.user(this.developerUserId).disable();

        StaffMember getted = this.staffService.user(this.developerUserId).get();
        assertThat(getted).isNotNull();
        assertThat(returned.isEnabled()).isFalse();
        assertThat(getted.isEnabled()).isFalse();
    }

    @Test
    void whenSetSlogan_sloganShouldBeSet() {
        this.staffService.updateIfPossible();
        String slogan = "wubba lubba dub dub";

        StaffMember returned = this.staffService.user(this.developerUserId).setSlogan(slogan);

        StaffMember getted = this.staffService.user(this.developerUserId).get();
        assertThat(getted).isNotNull();
        assertThat(returned.getSlogan()).hasValue(slogan);
        assertThat(getted.getSlogan()).hasValue(slogan);
    }

    @Test
    void whenRemoveSlogan_sloganShouldBeEmpty() {
        this.staffService.updateIfPossible();

        StaffMember returned = this.staffService.user(this.developerUserId).removeSlogan();

        StaffMember getted = this.staffService.user(this.developerUserId).get();
        assertThat(getted).isNotNull();
        assertThat(returned.getSlogan()).isEmpty();
        assertThat(getted.getSlogan()).isEmpty();
    }

    @Test
    void whenSetLink_linkShouldBeSet() {
        this.staffService.updateIfPossible();
        URI link = URI.create("https://example.org");

        StaffMember returned = this.staffService.user(this.developerUserId).setLink(link);

        StaffMember getted = this.staffService.user(this.developerUserId).get();
        assertThat(getted).isNotNull();
        assertThat(returned.getLink()).hasValue(link);
        assertThat(getted.getLink()).hasValue(link);
    }

    @Test
    void whenRemoveLink_linkShouldBeEmpty() {
        this.staffService.updateIfPossible();

        StaffMember returned = this.staffService.user(this.developerUserId).removeLink();

        StaffMember getted = this.staffService.user(this.developerUserId).get();
        assertThat(getted).isNotNull();
        assertThat(returned.getLink()).isEmpty();
        assertThat(getted.getLink()).isEmpty();
    }
}
