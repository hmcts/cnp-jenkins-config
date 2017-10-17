#!/bin/bash

echo "
rdr pass inet proto tcp from any to any port 8080 -> 192.168.0.7 port 8080
" | sudo -i pfctl -ef -

exit 0