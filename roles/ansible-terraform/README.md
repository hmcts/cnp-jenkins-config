terraform
=========

[![Build Status](https://travis-ci.org/kbrebanov/ansible-terraform.svg?branch=master)](https://travis-ci.org/kbrebanov/ansible-terraform)

Installs Terraform

Requirements
------------

This role requires Ansible 1.4 or higher.

Role Variables
--------------

| Name                | Default                                                          | Description                     |
|:--------------------|:-----------------------------------------------------------------|:--------------------------------|
| terraform_version   | 0.7.0                                                            | Version of Terraform to install |
| terraform_sha256sum | a196c63b967967343f3ae9bb18ce324a18b27690e2d105e1f38c5a2d7c02038d | SHA 256 checksum of package     |

Dependencies
------------

- kbrebanov.unzip

Example Playbook
----------------

Install Terraform
```
- hosts: all
  roles:
    - kbrebanov.terraform
```

License
-------

BSD

Author Information
------------------

Kevin Brebanov
