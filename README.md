# terraria-server-web

A web UI for Terraria server management.

## TODO

### MVP

- [x] 💙💜💛 Minimal project with basic authentication
- [x] 💙 Hosts register their state in the database
- [x] 💙💛 Host summary (on the front page)
- [x] 💙 Ability to fetch the current TModLoader versions from
  the [releases](https://github.com/tModLoader/tModLoader/releases/) of the tModLoader GitHub project
- [x] 💙💛 Ability to tell the host to download a Terraria dedicated server executable
  from [the Terraria wiki](https://terraria.gamepedia.com/Server#Downloads) and a TModLoader and combine them into a
  single directory
- [x] 💙 Keep track of the Terraria worlds of hosts
- [x] 💙💛 Display the Terraria worlds of hosts
- [x] 💙💛 Display the Terraria instances of a host
- [x] 💙💛 (#9) Ability to start and stop Terraria (one running instance per host) with a specific world. A process uses
  files as input/output and in the code and the database, it is represented as a state machine. It keeps track of its
  state and at which position it is in the input and output files. Alternatively, use `tmux`. That wouldn't be a
  cross-platform solution, but it would be much easier to implement.

### Basic features

- [x] 💙💛 (#15) Ability to download Terraria worlds of hosts
- [ ] 💙💛 Ability to upload Terraria worlds to hosts
- [x] 💙💛 (#30) Ability to delete Terraria worlds of hosts
- [ ] 💙💛 Ability to configure the Terraria executable path of a host ("${HOME}/.local/share/Terraria/{Instances |
  Executables}" by default) and to view what is present
- [ ] 💙💛 Display the Terraria mods of a host
- [ ] 💙💛 Ability to upload Terraria mods

### Advanced features

- 💙💛 ~~Ability to duplicate and rename Terraria worlds~~ - renaming turned out more complicated than expected
- [ ] 💙💛 Ability to download Terraria mods
- [x] 💙💛 Ability to start multiple Terraria instances per host ~~(every host has a quota)~~. ~~Different instances
  cannot use the same world.~~ Different instances can use the same world but there is a warning.
- [ ] 💙💛 Ability to use the tModLoader mod browser
- [ ] (Unrelated) Flyway migration tests with Docker

### Expert features

- [ ] (Unrelated) Flyway migration undo tests

---

#### Legend

- 💙 Backend
- 💜 Build / Infrastructure / Configuration
- 💚
- 💛 Frontend
- 💟 