# Jenkins CI

[![Build Status](https://travis-ci.org/geerlingguy/ansible-role-jenkins.svg?branch=master)](https://travis-ci.org/geerlingguy/ansible-role-jenkins)

Configuration for jenkins pipeline at HMCTS

## Shared library branch controls

`allowed-library-branches.yml` controls which `cnp-jenkins-library` branches may be used by pipelines. Temporary branches can define an optional `date_deadline` in `YYYY-MM-DD` format; usage generates deprecation warnings before the deadline and fails after it has passed.

```yaml
- name: feature/example
  allowed: true
  date_deadline: "2026-12-01"
```

## Requirements

Before creating a new Jenkins organisation, check you are observing the steps to [creating a Github respository](https://hmcts.github.io/ways-of-working/new-component/github-repo.html#create-a-github-repository)
This is to ensure the project will be created after triggering an organization scan, as the regex is designed to pick up certain patterns, [example regex](https://github.com/hmcts/cnp-jenkins-config/blob/master/jobdsl/organisations-beta.groovy#L14)

## Testing locally

See https://github.com/hmcts/cnp-jenkins-docker

## License

MIT
