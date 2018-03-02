# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.require_version ">= 2.0.0"
WORKSPACE = "../../"

Vagrant.configure("2") do |config|
  config.vm.box = "centos/7"
  config.vm.provision "shell", inline: "sudo systemctl stop firewalld", privileged: true

  config.vm.synced_folder ".", "/vagrant", type: "virtualbox"

  config.vm.provider "virtualbox" do |v|
    v.memory = 4096
    v.cpus = 2
    v.name = "jenkins-vm"
    v.customize ["modifyvm", :id, "--ioapic", "on", "--vram", "16"]
  end
  config.vm.hostname = "jenkins"
  config.vm.network :private_network, ip: "192.168.33.55"
  config.vm.network "forwarded_port", guest: 80, host: 8080
  config.vm.network "forwarded_port", guest: 8080, host: 8081


  config.vm.provision "ansible_local" do |ansible|
    ansible.playbook        = "playbook.yml"
    ansible.install         = true
    ansible.limit           = "all"
    ansible.inventory_path  = "hosts"
    ansible.version         = "latest"
    ansible.extra_vars      = "@externalvariables.yml"
  end

end
