# terraria-server-web

A web UI for Terraria server management.

## TODO

### MVP

- [x] 💙💜💛 Minimal project with basic authentication
- [x] 💙 Hosts register their state in the database
- [x] 💙💛 Host summary (on the front page)
- [ ] 💙💛 Ability to configure the Terraria executable path of a host ("${HOME}/.local/share/Terraria/{Instances |
  Executables}" by default) and to view what is present
- [ ] 💙💛 Ability to tell the host to download a certain tModLoader executable
  from [GitHub](https://github.com/tModLoader/tModLoader/releases/) through a specified three-part version
- [ ] 💙💛 Ability to tell the host to download a certain Terraria dedicated server executable
  from [the Terraria wiki](https://terraria.gamepedia.com/Server#Downloads) through any user-inputted URL that starts
  with "http://terraria.org/" or "https://www.terraria.org/"
- [ ] 💙💛 Display the Terraria worlds of a host
- [ ] 💙💛 Ability to start and stop Terraria (one instance per host) with a specific world. A process uses files as
  input/output and in the code and the database, it is represented as a state machine. It keeps track of its state and
  at which position it is in the input and output files.

### Basic features

- [ ] 💙💛 Ability to upload Terraria worlds
- [ ] 💙💛 Display the Terraria mods of a host
- [ ] 💙💛 Ability to upload Terraria mods

### Advanced features

- [ ] 💙💛 Ability to download Terraria worlds
- [ ] 💙💛 Ability to duplicate and rename Terraria worlds
- [ ] 💙💛 Ability to download Terraria mods
- [ ] 💙💛 Ability to start multiple Terraria instances per host (every host has a quota). Different instances cannot
  use the same world.
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