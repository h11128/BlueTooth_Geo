# BlueTooth_Geo
This is an Android Application that aims at indoor localization with bluetooth and inertial sensor.

# Current work
able to transmmit infomation via bluetooth  
able to get motion sensor data and orientation based on google api

# Current problem
orientation is not useful at all, I can get angle of the device between y and north, which is called azimuth. But this angle is not useful because I want the angle between two postion not two device. Suppose I holding a phone turn around, the angle between two position is not chaning but between two device change. unless we have location of two device so that we can infer the angle between two device.  

Our task is to find the distance and angle with two device. But this is not easy. For angel I think we need a server and map in this environment, and we need to set the original position of our device manually. The scenario we want to invesitigate seems like don't need a map. So I begin to get confused. I don't think this is a easy way and I don't know whether we need to investigate this or find another useful way. Becasue If we only want the distance there are many other open source library that use bluetooth, wifi, sound, light bulb, visual information, which is completely different system. I don't know whether we should insisit on imu or just whatever that can track the location

motion sensor data unable to recognize the relative location between two device, only able to get fingerprinting of movement by dead reckoning. 

The main existing open source library for this problem use bluetooth, wifi, light bulb, ultra sound, visual information. The motion sensor data only provide supplementary work, not existing code use imu to find the distance between two device
