![icon](https://github.com/user-attachments/assets/1dc24087-758a-4508-b38e-550fd40ade87)

# What is this?

### Too long, didn't read:
A service which lets Roblox connect with a Discord Bot, lets mods run in-game commands from Discord, logs moderator activity, supports both player report and ban appeal tickets, and keeps track of bans and ban evidence.

### Too long, did read:
A Java application which runs both a [Spring Boot](https://spring.io/projects/spring-boot) web API, and a Discord Bot with [JDA](https://github.com/discord-jda/JDA). In tandem, these two services drive the moderation side of the Roblox game [Ability Wars](https://www.roblox.com/games/8260276694/UPDATE-Ability-Wars). I developed this as a portfolio-building exercise, learning experience for Spring Boot, and just for fun. 😁 Despite this though, this software is an absolute powerhouse with deep integration and pretty solid infrastructure. I think there is some scalability issues when it comes to how the database code works, but overall it's really solid and fast.

### Documentation
I'm releasing this to hopefully help out someone interested in doing something similar for their own game with a similar tech stack. All the source code is documented and commented to the BEST of my ability to hopefully enable easy understanding and modification. It's REALLY biased for Ability Wars, but that's just because that's what it's designed for. Hack this code up all you want and make it your own. There's plenty of examples here to learn from!

# Features
- Allows players to query their in-game stats, and status of their in-game ban, if any.
- Allows moderators to run in-game commands from Discord, including banning users, unbanning users, and setting their punch count.
- Tracks and logs moderation actions with database storage and webhook-based logging.
- Implements ticketing with deep integration with the game.
  - Multiple layers of validation to keep user error as low as possible.
  - Integrates with tickets to link evidence to bans and recall detailed information about specific infractions.
  - Has easy one-click actions for moderators to use to resolve tickets auto-magically.
  - Commands to modify parts of a ticket intelligently.
- Extremely secure moderator validation and querying of in-game permissions.
- Allows the admins to quickly count the number of valid bans done in a week period for payment.
- Bloxlink integration to make commands as foolproof as possible.
- Discord and Roblox account-based blacklisting for appeals and a rich incident lookup system for recalling information.

# How do I use it?
If you don't have experience in Java/JDA/Spring or it's not immediately obvious how to setup this project and begin working on it, then this project might not be the pick for you.
You're fully allowed to use this for your own Roblox game (within license terms), but it will require **lots** of effort to deploy and hook it up to your game. 

### Polling
Once you have the Spring application exposed on a server/VPS, you'll need to call the `/poll` endpoint every couple of seconds and `/fulfill` the requests accordingly.
You can make/receive more than one request per call on either of the endpoints. Additionally, it's good to note you can `/fulfill` anything that happens, even if you don't have a fulfillment ID.

### Environment Variables
Besides changing the ID constants internally, you'll also need your environment set up with a couple of environment variables:
- `AW_DB_URL` The URL of the SQLite database to use.
- `BLOXLINK_API_KEY` Your [BloxLink API key](https://blox.link/dashboard/user/developer).
- `AW_DEBUG` If debug should be enabled (0 or 1). Commands are re-registered every time the bot boots with this on as well as some extra logging.
  - I personally enable this on a specific IntelliJ launch profile.
- `AW_BOT_TOKEN` The Discord bot token.
- `AW_API_KEY` The private API key that's only present on the host server and Roblox server.
