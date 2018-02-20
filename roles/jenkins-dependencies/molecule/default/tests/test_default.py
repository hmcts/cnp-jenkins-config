import os

import testinfra.utils.ansible_runner

testinfra_hosts = testinfra.utils.ansible_runner.AnsibleRunner(
    os.environ['MOLECULE_INVENTORY_FILE']).get_hosts('all')


def test_hosts_file(host):
    f = host.file('/etc/hosts')

    assert f.exists
    assert f.user == 'root'
    assert f.group == 'root'


def test_nodejs_is_installed(host):
    p = host.package("nodejs")

    assert p.is_installed
    assert p.version == '8.9.4'


def test_epel_release_is_installed(host):
    p = host.package("epel-release")

    assert p.is_installed


def test_python_pip_is_installed(host):
    p = host.package("python-jenkins")

    assert p.is_installed
