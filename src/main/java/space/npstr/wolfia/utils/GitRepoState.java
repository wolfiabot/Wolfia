/*
 * Copyright (C) 2016-2025 the original author or authors
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

package space.npstr.wolfia.utils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Provides access to the values of the property file generated by whatever git info plugin we're using
 */
public class GitRepoState {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GitRepoState.class);

    public static GitRepoState getGitRepositoryState() {
        return GitRepoStateHolder.INSTANCE;
    }

    // https://github.com/n0mer/gradle-git-properties/issues/71
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    //holder pattern
    private static final class GitRepoStateHolder {
        private static final GitRepoState INSTANCE = new GitRepoState("git.properties");
    }

    public final String branch;
    public final String commitId;
    public final String commitIdAbbrev;
    public final long commitTime; //epoch seconds

    @SuppressWarnings("ConstantConditions")
    public GitRepoState(String propsName) {

        Properties properties = new Properties();
        try {
            properties.load(GitRepoState.class.getClassLoader().getResourceAsStream(propsName));
        } catch (NullPointerException | IOException e) {
            log.info("Failed to load git repo information", e); //need to build with build tool to get them
        }

        this.branch = String.valueOf(properties.getOrDefault("git.branch", ""));
        this.commitId = String.valueOf(properties.getOrDefault("git.commit.id", ""));
        this.commitIdAbbrev = String.valueOf(properties.getOrDefault("git.commit.id.abbrev", ""));
        String time = String.valueOf(properties.get("git.commit.time"));
        if (time == null) {
            this.commitTime = 0;
        } else {
            this.commitTime = OffsetDateTime.from(DTF.parse(time)).toEpochSecond();
        }
    }
}
