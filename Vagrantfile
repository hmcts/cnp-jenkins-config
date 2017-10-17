# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.require_version ">= 2.0.0"
WORKSPACE = "../../"

Vagrant.configure("2") do |config|
  config.vm.box = "puppetlabs/centos-7.2-64-nocm"
  if (/cygwin|mswin|mingw|bccwin|wince|emx/ =~ RUBY_PLATFORM) != nil
    config.vm.synced_folder WORKSPACE, "/Workspace", mount_options: ["dmode=700,fmode=600"]
  else
    config.vm.synced_folder WORKSPACE, "/Workspace"
  end

  config.vm.provider "virtualbox" do |v|
    v.memory = 2048
    v.name = "jenkins-vm"
  end

  # Redirections on OSX Host machine - requires vagrant plugin trigger
  # config.trigger.after [:up, :resume] do
  #   run "./portredirect.sh"
  # end
  #
  # config.trigger.after :halt do
  #   run "./portredirect_disable.sh"
  # end
  
  config.vm.network "forwarded_port", guest: 8080, host: 8080
  # config.ssh.shell = "bash -c 'BASH_ENV=/etc/profile exec bash'"
  # config.ssh.forward_agent = true

  # config.vm.provision "file", source: "~/.ssh/", destination: "$HOME/.ssh"

  config.vm.provision "ansible_local" do |ansible|
    ansible.playbook        = "playbook.yml"
    ansible.verbose        = false
    ansible.install        = true
    ansible.limit          = "all"
    ansible.inventory_path = "hosts"
  end
end
