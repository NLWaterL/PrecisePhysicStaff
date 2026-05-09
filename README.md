Description
---
A mod that adds a powerful tool to manipulate the physics contraptions from Sable.

Requires Create, Create Aeronautics and Sable to work.

Features
---
The Precise Physics Staff, the only item added by the mod, gives fine-grained control over any Sable physics contraption.

### Basic controls

- **Right-click** at a contraption to select it. The staff will hold it in place.
- **Left-click** to lock / unlock a contraption (same as the creative physics staff).
- **R** toggles between *Movement* and *Rotation* mode while a contraption is selected.

### Movement mode 

The direction is camera-relative.

| Key               | Action                  |
|-------------------|-------------------------|
| `W` / `S`         | Move forward / backward |
| `A` / `D`         | Move left / right       |
| `Space` / `Shift` | Move up / down          |

### Rotation mode

Rotations are performed around **world axis** derived from your view direction.

| Key                       | Action                                                |
|---------------------------|-------------------------------------------------------|
| `W` / `S`                 | Pitch (rotate around the view-right horizontal axis)  |
| `A` / `D`                 | Yaw (rotate around the world Y axis)                  |
| `Shift + A` / `Shift + D` | Roll (rotate around the view-forward horizontal axis) |
| `Hold Ctrl`               | Step in 90° increments                                |

### Other controls

- **Hold Tab** for finer movements / rotations.

### Command mode

Press **Enter** or **/** while a contraption is selected to open the command input. Commands are case-insensitive.

#### Rotation commands

Positive values rotate clockwise from the indicated viewpoint.

| Command      | Axis                                                                     |
|--------------|--------------------------------------------------------------------------|
| `RX [Angle]` | Contraption-local X (clockwise viewed from +X)                           |
| `RY [Angle]` | Contraption-local Y (clockwise viewed from above)                        |
| `RZ [Angle]` | Contraption-local Z (clockwise viewed from +Z)                           |
| `R [Angle]`  | The contraption-local axis that best matches your current look direction |

#### Movements commands

| Command                            | Direction                                                                                                         |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `X` / `Y` / `Z` `[Distance]`       | Move along world X / Y / Z axis                                                                                   |
| `W` / `A` / `S` / `D` `[Distance]` | Move forward / left / backward / right relative to your view (snapped to the nearest world axis; horizontal only) |

#### Utility commands

| Command          | Behavior                                          |
|------------------|---------------------------------------------------|
| `GRID` (or `G`)  | Snap the contraption to the block grid            |
| `HELP <command>` | Show in-game documentations for the given command |


License
---
This project is under MIT License.
