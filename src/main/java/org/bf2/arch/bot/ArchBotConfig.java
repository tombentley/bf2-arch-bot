package org.bf2.arch.bot;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Config file for the bot.
 * Lives in {@code .github/bf2-arch-bot.yml}.
 */
public class ArchBotConfig {

    /**
     * Github login name of the bot itself.
     */
    String botUserLogin;

    /**
     * The time, in minutes, to wait between checking for stalled discussions.
     */
    long stalledDiscussionPollTimeMins;

    /**
     * Github logins of people who can do "/create adr" etc. on issues.
     */
    @JsonDeserialize(as = TreeSet.class)
    Set<String> recordCreationApprovers = new TreeSet<>();

    /**
     * The URL at which the site is published.
     */
    String publishedUrl = "https://architecture.appservices.tech";

    @Override
    public String toString() {
        return "ArchBotConfig(" +
                "botUserLogin='" + botUserLogin + '\'' +
                ", stalledDiscussionPollTimeMins=" + stalledDiscussionPollTimeMins +
                ", recordCreationApprovers=" + recordCreationApprovers +
                ", publishedUrl='" + publishedUrl + '\'' +
                ')';
    }
}
