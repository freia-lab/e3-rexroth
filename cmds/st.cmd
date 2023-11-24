# This should be a test or example startup script

require rexroth

# Limit the error messages form the StreamDevice (specially when the device is not connected)
var streamErrorDeadTime 60

iocshLoad("$(rexroth_DIR)rexroth.iocsh", "IP=192.168.10.108,P=CstatV-AC:,IOCNAME=ioc26-rexroth")
