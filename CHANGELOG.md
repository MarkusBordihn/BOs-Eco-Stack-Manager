# Changelog for Eco Stack Manager

## Note

This change log includes the summarized changes.
For the full changelog, please go to the [GitHub History][history] instead.

### 1.2.0

- Added basic configuration files under /config/eco_stack_manager to fine-tune settings.
- Added allow list to limit the optimization to specific items.
- Added deny list to exclude specific items from the optimization.
- Added feature to move existing experience orbs to the new "drop" location.
- Added feature to move existing item entities to the new "drop" location.
- Improved code quality and added more detailed debug messages.

### 1.1.0

- Added '/eco_stack_manager debug <true|false>' command to enable or disable debug mode.
- Added automatic item entity cleanup to prevent memory leaks.
- Added warning message and automatic deactivation for features which are incompatible with other
  mods.
- Improved memory usage and performance by reducing the number of unnecessary calculations.

### 1.0.0

- Released first beta version for more detailed live testing.

[history]: https://github.com/MarkusBordihn/BOs-Eco-Stack-Manager/commits/
