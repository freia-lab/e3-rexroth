# This should be a test or example startup script

require rexroth

# Limit the error messages form the StreamDevice (specially when the device is not connected)
var streamErrorDeadTime 60

iocshLoad("$(rexroth_DIR)rexroth.iocsh", "IP=192.168.10.126,P=CstatV-AC:,IP_PORT=2195,IOCNAME=ioc26-rexroth")
