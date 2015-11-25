#! /usr/bin/env python

import multiprocessing
import subprocess
import webbrowser
import sys
import os

def user_is_root():
    '''A portable way to check if the user is root.'''
    root_location_file = '/etc/___nonsense___.foo.txt'
    try:
        open(root_location_file, 'w').write('hello world')
    except IOError:
        return False
    os.remove(root_location_file)
    return True


def environment_language():
    '''Return the first of CENOLANG or LANGUAGE that is set else en-us if none are'''
    possible_vars = ['CENOLANG', 'LANGUAGE']
    options = map(lambda variable: os.environ.get(variable, None), possible_vars)
    set_options = filter(lambda value: value is not None, options)
    if len(set_options) == 0:
        return 'en-us'
    return set_options[0]


def start_browser():
    '''Start the user's default browser with the instructions page for installing the extension'''
    home = os.environ['HOME']
    extension_install_page = 'file://' + home + '/CENOBox/views/extension-en-us.html'
    webbrowser.open(extension_install_page)


def run_freenet():
    '''Run the Freenet-handling run script in a thread, writing output to a file'''
    with open('freenet.log', 'w') as log_file:
        subprocess.call(['sh', 'run.sh', 'start'], stdout=log_file)


def run_ceno():
    '''Run the CENO Client in a subprocess, writing output to a file'''
    with open('client.log', 'w') as log_file:
        subprocess.call(['./CENOClient'], stdout=log_file)


def start_ceno():
    '''Start Freenet and CENOClient and save information about the latter's process id'''
    freenet_process = multiprocessing.Process(target=run_freenet)
    # Check if CENO is already running
    for line in subprocess.Popen(['ps', 'ax'], stdout=subprocess.PIPE).stdout:
        if 'grep' not in line and 'CENOClient' in line:
            print 'CENOClient is already running'
            return
    ceno_process = multiprocessing.Process(target=run_ceno)
    try:
        freenet_process.start()
        print 'Freenet is running'
        ceno_process.start()
        print 'CENOClient is running'
        cc_pid = ceno_process.pid
        open('CENOClient.pid', 'w').write(str(cc_pid))
        freenet_process.join()
        ceno_process.join()
    except:
        freenet_process.terminate()
        print 'Terminated Freenet'
        ceno_process.terminate()
        print 'Terminated CENOClient'


def main():
    if len(sys.argv) > 1 and sys.argv[1].lower() == 'stop':
        subprocess.call(['sh', 'run.sh', 'stop'])
        if os.path.isfile('CENOClient.pid'):
            pid = open('CENOClient.pid').read()
            subprocess.call(['kill', pid])
            print 'Stopped CENO Client proxy'
    else:
        ceno_language = environment_language()
        os.environ['CENOLANG'] = ceno_language
        print 'CENO language set to ' + ceno_language
        start_browser()
        start_ceno()


if __name__ == '__main__':
    main()
