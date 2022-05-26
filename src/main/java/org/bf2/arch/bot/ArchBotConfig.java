package org.bf2.arch.bot;

import java.util.List;

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
     * The time, in hours, to wait between checking for stalled discussions.
     */
    long stalledDiscussionPollTimeMins;

    /**
     * Github logins of people who can do "/create adr" etc. on issues.
     */
    List<String> recordCreationApprovers;

    /**
     * The URL at which the site is published.
     */
    String publishedUrl = "https://architecture.appservices.tech";

}
