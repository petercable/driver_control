
import time
import logging
from zmq_client import ZmqDriverClient
from IPython import embed
import docopt

SLEEPTIME = .5


__doc__ = 'Usage: driver_control.py <host> <command_port> <event_port>'

massp_config = {
    'mcu': {
        'addr': 'localhost',
        'port': 6007,
        'cmd_port': 6008
    },
    'turbo': {
        'addr': 'localhost',
        'port': 6004,
        'cmd_port': 6005
    },
    'rga': {
        'addr': 'localhost',
        'port': 6001,
        'cmd_port': 6002
    },
}

massp_startup_config = {
    'parameters':
        {'rga_steps_per_amu': 10,
         'mcu_one_minute': 1000,
         'rga_initial_mass': 1,
         'rga_filament_emission_set': 1.0,
         'massp_sample_interval': 3600,
         'turbo_max_temp_bearing': 65,
         'turbo_target_speed': 90000,
         'turbo_status_update_interval': 5,
         'rga_final_mass': 100,
         'turbo_max_temp_motor': 90,
         'rga_ion_energy': 1,
         'rga_high_voltage_cdem': 0,
         'rga_focus_voltage': 90,
         'mcu_telegram_interval': 10000,
         'turbo_max_drive_current': 140,
         'mcu_sample_time': 10,
         'rga_electron_energy': 70,
         'turbo_min_speed': 80000,
         'rga_noise_floor': 3
        }
}

def get_logger():
    logger = logging.getLogger('driver_control')
    logger.setLevel(logging.DEBUG)

    # create console handler and set level to debug
    ch = logging.StreamHandler()
    ch.setLevel(logging.DEBUG)

    # create formatter
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')

    # add formatter to ch
    ch.setFormatter(formatter)

    # add ch to logger
    logger.addHandler(ch)
    return logger

log = get_logger()

def s():
    time.sleep(.001)

def main():
    options = docopt.docopt(__doc__)
    host = options['<host>']
    command_port = int(options['<command_port>'])
    event_port = int(options['<event_port>'])

    #z = ZmqDriverClient(host, command_port, event_port)
    #z.start_messaging(callback)
    #z.ping()
    # z.configure(massp_config)
    # z._command('set_init_params', massp_startup_config)
    # z.connect()
    embed()
    #z.stop_messaging()


def callback(data):
    log.debug('DATA: %s', data)

if __name__ == '__main__':
    main()
