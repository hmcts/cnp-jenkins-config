# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.require_version ">= 2.0.0"
WORKSPACE = "../../"


$set_environment_variables = <<SCRIPT
tee "/etc/profile.d/myvars.sh" > "/dev/null" <<EOF
# Ansible environment variables.
  export AZURE_CLIENT_ID=#{ENV['AZURE_CLIENT_ID']}
  export AZURE_VAULT_URI=#{ENV['AZURE_VAULT_URI']}
  export AZURE_SECRET=#{ENV['AZURE_SECRET']}
  export AZURE_TENANT=#{ENV['AZURE_TENANT']}
EOF
SCRIPT


Vagrant.configure("2") do |config|
  config.vm.box = "centos/7"
  config.vm.provision "shell", inline: "sudo systemctl stop firewalld", privileged: true
  config.vm.provision "shell", inline: $set_environment_variables, run: "always"

  config.vm.provider "virtualbox" do |v|
    v.memory = 4096
    v.cpus = 2
    v.name = "jenkins-vm"
    v.customize ["modifyvm", :id, "--ioapic", "on", "--vram", "16"]
  end
  config.vm.hostname = "jenkins"
  config.vm.network :private_network, ip: "192.168.33.55"
  config.vm.network "forwarded_port", guest: 80, host: 8080
  config.vm.network "forwarded_port", guest: 443, host: 1443
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
