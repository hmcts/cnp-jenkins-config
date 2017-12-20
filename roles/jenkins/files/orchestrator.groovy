node {
  echo 'Hello World'

  environmnent = 'prod'
  //build core infra
  build job: 'contino/moj-core-infrastructure/parametrised',
      parameters: [string(name: 'PRODUCT_NAME', value: String.valueOf('core-infra')),
                   string(name: 'ENV_NAME', value: environmnent)]

  //build core compute
  build job: 'contino/moj-core-compute/parametrised',
      parameters: [string(name: 'PRODUCT_NAME', value: String.valueOf('core-compute')),
                   string(name: 'ENV_NAME', value: environmnent)]

  //deploy rhubarb
  build job: 'contino/moj-rhubarb-infrastructure/parametrised',
      parameters: [string(name: 'PRODUCT_NAME', value: String.valueOf('rhubarb')),
                   string(name: 'ENV_NAME', value: environmnent)]

  parallel {

    build job: 'contino/moj-rhubarb-frontend/parametrised',
        parameters: [string(name: 'PRODUCT_NAME', value: String.valueOf('rhubarb')),
                     string(name: 'ENV_NAME', value: environmnent)]

    build job: 'contino/moj-rhubarb-recipes-service/parametrised',
        parameters: [string(name: 'PRODUCT_NAME', value: String.valueOf('rhubarb')),
                     string(name: 'ENV_NAME', value: environmnent)]
  }

}