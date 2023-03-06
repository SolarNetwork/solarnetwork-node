# SolarNode Core Security

This plugin provides core security services for SolarNode.

## Password encoder

This plugin provides a [`PasswordEncoder`][PasswordEncoder] service for hashing SolarNode login
passwords. It uses the [bcrypt][bcrypt] algorithm.

## User details service

This plugin provides a [`UserDetailsService`][UserDetailsService] service for persisting SolarNode
login credentials. The persistence mechanism has evolved over time.

### The `users.json` file

The current persistence method uses a simple [JSON][json] file to store the login credentials, which by
default is saved in a `conf/users.json` file. In SolarNodeOS the path is `/etc/solarnode/users.json`.
The file format is a JSON array of _user_ objects. Each user object contains the following properties:

| Property | Type | Description |
|:---------|:-----|:------------|
| `created` | number | The user object's creation date, as a millisecond Unix epoch value. |
| `modified` | number | The user object's last modification date, as a millisecond Unix epoch value. |
| `username` | string | The login username. |
| `password` | string | The login password, hashed using the [`PasswordEncoder`](#password-encoder) service. |
| `roles` | array\<string\> | The list of authorization roles (groups) assigned to the user. |

#### Example `users.json` file

An example `users.json` file looks like this:

```json
[
  {
    "created":  1678072630113,
    "modified": 1678074554064,
    "username": "example@example.com",
    "password": "$2a$10$/UuHuIflOQv6RmuNyFfLYO47WsbdUQpgKX6Tc0Ghy9/ObbQ6d/LEW",
    "roles":    ["ROLE_USER"]
  }
]
```

SolarNode will automatically migrate account information stored in the older methods outlined
below into the `users.json` file.

### Settings table

SolarNode used to store login credentials in the main database's `settings` table. This table is used
to hold all user-configured runtime settings, and has a generic structure not specific to persisting
account information.

| Column | Description |
|:-------|:------------|
| `key` | The login username. |
| `type` | Either `solarnode.user` for a password record, or `solarnode.role` for a role record. |
| `value` | Either the login password, hashed using the [`PasswordEncoder`](#password-encoder) service, or an authorization role assigned to the user. |
| `flags` | Unused. |
| `modified` | The record's last modification date. |


#### Example settings records

Using the same information from the [`users.json` file example](#example-usersjson-file), settings records
like the following would be created:

```csv
key,type,value,flags,modified
example@example.com,solarnode.role,ROLE_USER,0,2023-03-06 07:18:30
example@example.com,solarnode.user,$2a$10$/UuHuIflOQv6RmuNyFfLYO47WsbdUQpgKX6Tc0Ghy9/ObbQ6d/LEW,0,2023-03-06 07:18:30
```

### Fixed credentials

Originally SolarNode did not support multiple login credentials; the username was fixed to the node's ID and
the password was hard-coded as `solar`.


[bcrypt]: https://en.wikipedia.org/wiki/Bcrypt
[json]: https://www.json.org
[PasswordEncoder]: https://docs.spring.io/spring-security/site/docs/4.2.x/reference/html/core-services.html#core-services-password-encoding
[UserDetailsService]: https://docs.spring.io/spring-security/site/docs/4.2.x/reference/html/appendix-faq.html#appendix-faq-what-is-userdetailservice