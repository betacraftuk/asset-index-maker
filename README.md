# Usage
```sh
java -jar asset-index-maker.jar [arguments ...]
```
## Arguments
`--resources`
- if provided, `map_to_resources` will be set to `true`

`--virtual`
- if provided, `virtual` will be set to `true`

`--directory=[assets directory]`
- if provided, will use the directory as a source of assets to generate an index for
- if not provided, will default to the current working directory

`--output=[asset index json path]`
- if provided, will output the asset index json to that path
- if not provided, will default to `index.json`

`--testAvailability`
- if provided, will test assets for being downloadable from the official Mojang server

`--exportMissing[=[missing assets directory]]`
- if provided, will export assets missing from the Mojang server to a separate directory
- if not set, the directory will default to `assets_missing` relative to current working directory
- requires `--testAvailability`

`--asObjects`
- if provided, will export missing assets in "objects structure", e.g. `assets_missing/02/022e48005197b37f0caa53e8051c8a266eeae15e` instead of `assets_missing/newsound/random/door_close.ogg`
- requires `--testAvailability` and `--exportMissing`

`--customUrl=[url prefix]`
- if provided, will add `custom_url` property to each asset which is not downloadable from the Mojang server
- example download url: `[url prefix]/02/022e48005197b37f0caa53e8051c8a266eeae15e`
- requires `--testAvailability`
- almost all launchers don't support this property

`--debug`
- if provided, will print additional debugging information
