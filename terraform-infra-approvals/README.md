# Terraform  whitelists folder

A whitelist is a list of Terraform resources and modules used to enforce constraints on infrastructure creation. 
Whitelist files can be global, in which case it is named `global.json` or specific to a github repo and named `<repo-name>.json`. 

An example whitelist file could be as follows:

```json
{
  "resources": [
    {"type": "azurerm_key_vault_secret"},
    {"type": "azurerm_resource_group"}
  ],
  "module_calls": [
    {"source":  "git@github.com:hmcts/cnp-module-webapp?ref=master"},
    {"source":  "git@github.com:hmcts/cnp-module-postgres?ref=master"}
  ]
}
```

To allow infrastructure builds to progress, its definition is matched against a whitelist of resources and modules, 
using the following command:

```shell script
tf-utils --whitelist <terraform-infra-dir-path> <whitelist-file-path>...<whitelist-file-path>
```

For more information see: [terraform-config-inspect](https://github.com/hashicorp/terraform-config-inspect)


