#!/bin/bash

echo "Disabling vagrant port forwarding..."
sudo -i pfctl -F all -f /etc/pf.conf

exit 0