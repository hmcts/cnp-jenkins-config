# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.require_version ">= 2.0.0"
WORKSPACE = "../../"

Vagrant.configure("2") do |config|
  config.vm.box = "puppetlabs/centos-7.2-64-nocm"
  #config.vm.box = "puppetlabs/ubuntu-16.04-64-nocm"
  if (/cygwin|mswin|mingw|bccwin|wince|emx/ =~ RUBY_PLATFORM) != nil
    config.vm.synced_folder WORKSPACE, "/Workspace", mount_options: ["dmode=700,fmode=600"]
  else
    config.vm.synced_folder WORKSPACE, "/Workspace"
  end

  config.vm.provider "virtualbox" do |v|
    v.memory = 4096
    v.cpus = 2
    v.name = "jenkins-vm"
    v.customize ["modifyvm", :id, "--ioapic", "on", "--vram", "16"]
  end

  config.vm.network "forwarded_port", guest: 8080, host: 8080
  config.vm.network "forwarded_port", guest: 2020, host: 2020
  # config.ssh.shell = "bash -c 'BASH_ENV=/etc/profile exec bash'"
  # config.ssh.forward_agent = true

  config.vm.provision "shell",
                      inline: "sudo systemctl stop firewalld",
                      privileged: true

  config.vm.provision "ansible_local" do |ansible|
    ansible.playbook        = "playbook.yml"
    ansible.verbose        = true
    ansible.install        = true
    ansible.limit          = "all"
    ansible.inventory_path = "hosts"
    ansible.extra_vars = "@local_env.json"
  end

end
