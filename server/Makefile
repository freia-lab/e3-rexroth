# Makefile for Java program

# Run the program with the command:
# make run SERVER_PORT=<s_port> IP_ADDR=<ip_addr>
# where:
# s_port - port number on which the server listens to the connectio from the Epics IOC
# ip_addr - IP address of the motor controller

# Set the main class name
MAIN_CLASS = StreamDevServer

EAL_SDK = /usr/local/eal_sdk/02VRS/eal4Java
#EAL_SDK = /opt/eal4Java

JAVA_HOME= /usr/lib/jvm/java-11-openjdk-amd64

# Set the source directory
SRC_DIR = src

# Set the build directory
BUILD_DIR = build

# Set the output directory
OUT_DIR = out

# Set the Java compiler
JAVAC = javac

# Set the Java compiler flags
JFLAGS = -d $(BUILD_DIR) -sourcepath $(SRC_DIR) -cp $(EXTERNAL_LIBS)

# Set the Java virtual machine
JAVA = java

# Set the classpath
CLASSPATH = $(BUILD_DIR):/usr/share/java/*:$(JAVA_HOME)/lib/*:$(EAL_SDK)/lib/*

# List of all Java source files
SOURCES = $(wildcard $(SRC_DIR)/*.java)

# List of all class files
CLASSES = $(patsubst $(SRC_DIR)/%.java,$(BUILD_DIR)/%.class,$(SOURCES))

# Set the external JAR libraries
EXTERNAL_LIBS = $(EAL_SDK)/lib/libEALJavaWrapper.jar:/usr/share/java/commons-lang3-3.11.jar

# Default target
all: $(OUT_DIR)/$(MAIN_CLASS).jar

# Compile the Java source files
$(BUILD_DIR)/%.class: $(SRC_DIR)/%.java
	$(JAVAC) $(JFLAGS) $<

# Create a JAR file
$(OUT_DIR)/$(MAIN_CLASS).jar: $(CLASSES)
	jar cvfe $@ $(MAIN_CLASS) -C $(BUILD_DIR) .

# Run the $(MAIN_CLASS)
run: $(OUT_DIR)/$(MAIN_CLASS).jar
	$(JAVA) -cp $(CLASSPATH) $(MAIN_CLASS) $(SERVER_PORT) $(DRIVER_PORT)

# Clean up
clean:
	rm -rf $(BUILD_DIR)/* $(OUT_DIR)/*

# Phony targets
.PHONY: all run clean
