# Test Procedures for OOI Drivers

DriverControl provides an interface for testing of OOI instrument protocol drivers. 

# Setup

You will need a working marine integrations environment prior to launching any drivers.
See [https://github.com/ooici/marine-integrations](https://github.com/ooici/marine-integrations)

## Configuration File

Prior to testing you must setup a configuration file containing the following yaml values:

**scenario** The scenario is the name of the driver as specified in the preload sheet. This

The configuration file can be provided as an argument to Driver Control using the argument 
'--config-file=<config_file_path>'.

**port_agent_config** The following parameters must be specified for the port agent:

* **addr** Port agent address. Should be set to *rsn-port-agent-test.oceanobservatories.org*.
* **port** Port agent port. This is driver dependent. This is specified on the 
[RSN agent supervisor webpage](http://rsn-port-agent-test.oceanobservatories.org:9001).
* **cmd_port** Port agent command port. Similar to the agent port. Should be 1000 less than the agent port. 

**startup_config** The following parameters must be specified for the startup configuration:

* **parameters** A full set of startup parameters can be provided for the driver to load on startup.

The following driver configuration file sets up testing for the TRHPH instrument driver:

```
    driver_config:
      scenario: THSPH_A
    port_agent_config:
      addr: rsn-port-agent-test.oceanobservatories.org
      port: 13008
      cmd_port: 12008
    startup_config:
      parameters:
```

# Driver Test


**Do this once**

```
    workon ooi
    pip install openpyxl docopt
```

**Do this in every new shell where you wish to run driver_control**

```
    workon ooi
    export TEST_BASE=/path/to/marine/integrations
```

**Launch Driver Control**

```
    ./gradlew run
```

Driver Config allows step by step execution of instrument driver commands. The protocol driver must be executed 
in the following sequence:

1. **Load test configuration**. Test configuration can be loaded two ways:
  * Specify the test configuration file on the command line (*--config=<config file>*).
```
    ./gradlew run -PappArgs="['--config=src/main/resources/thsph.yaml']"
```
  * Use the menu option *File->Load Config*.
  
2. **Launch the driver**. The instrument driver must be running prior to testing, although it is not necessary to 
start it from within Driver Control if it is already running.

3. **Connect to the driver**. Select the menu option *Test->Connect to Driver*. The driver log file will 
indicate a successful connection.  The bottom left status box will indicate the current driver state. 

  * Driver Control will attempt to exercise the driver through DISCOVER and into a known state.  If this fails
    you can step through the individual steps through the "Driver" menu.

4. **Configure the driver**. If the driver state is *DRIVER_STATE_UNCONFIGURED*, send the instrument driver the 
configure command using menu option *Driver->Configure*. This can be verified in the driver log file.

5. **Connect to instrument**. If the driver state is *DRIVER_STATE_DISCONNECTED*, send the connect command to the 
instrument driver using the menu option *Driver->Connect to Instrument*. This can be verified in the driver log 
file.

6. **Discover state**. If the driver state is *DRIVER_STATE_UNKNOWN* the driver needs to enter discovery to 
determine its state. Enter discovery by selecting the menu option *Driver->Discover*. Depending on the instrument
it may take some time to establish first communications with the instrument through the driver. The bottom right 
status text will display *sending command discover_state...* until the discovery is complete. At the completion of the discovery phase, the driver state should be *DRIVER_STATE_COMMAND* or *DRIVER_STATE_AUTOSAMPLE* depending on the
instrument. 

7. **Testing**. There are three main components to driver testing:

  * **State Change**. Verify successful connection to the driver and exercise the various capabilities in each
    state. This is accomplished on the *Command* tab. 

  * **Parameter Management**. Verify parameters can be initialized, read, and set. 
  
    1. *Parameter initialization.* Parameter startup values are specified in the test configuration file. Those
       values must match the values displayed in the *Parameters* tab after a clean startup of the driver. 

    2. *Parameter read.* Verify all instrument parameters are listed in the *Parameters* tab. The columns can be 
       sorted to assist in matching parameters. 
       
    3. *Parameter set.* Verify modifiable instrument parameters can be set and read-only parameters cannot be
       set. To set a parameter value, first select the parameter row, then click in the New Value cell. After 
       setting all parameters, press the *Send* button. 
       
      * You must press *Enter* in the cell for the parameter value to be saved.
      
  * **Stream validation**. Verify the streams are published as expected and produce the correct values. 
    Select *Test->Validate Streams* after collecting stream data from the driver to compare against the values
    supplied to the Preload spreadsheet.
    
  * **Algorithm validation**. Verify data published by the streams can be used to provide data to algorithms. 
    
    * *This is feature is only partially implemented in Driver Control.*
      
