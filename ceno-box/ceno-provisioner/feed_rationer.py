#!/usr/bin/python2

"""
This script is taking the feed file and number of feeders and divide the feed file into 
n feed file where n is the number feeding edges.
"""


import optparse
import pdb

def generate_subfeed_files(main_feed_filename, subfeed_file_prefix, no_feeders):

    subfeed_files = []

    feed_no = 0
    with open(main_feed_filename) as feeds:
        #open all feed output
        for i in range(no_feeders):
            subfeed_files.append(open(subfeed_file_prefix+str(i)+".txt", 'w'))

        for cur_feed in feeds:
            subfeed_files[feed_no % no_feeders].write(cur_feed)
            feed_no+=1
            

def main():
    parser = optparse.OptionParser()
    
    parser.add_option("-f", "--feed-file", dest="feed_file",
                      help="The name of the feed file",
                      default="feedlist.txt",
                      action="store"
                      )

    parser.add_option("-s", "--subfeed-file-prefix", dest="subfeed_file_prefix",
                      help="The name of the feed file",
                      default="subfeedlist",
                      action="store"
                      )

    parser.add_option("-n", "--no-feeders", dest="no_feeders",
                      help="The number of feeder nodes",
                      default=1,
                      action="store")

    (parsed_options, args) = parser.parse_args()
    
    generate_subfeed_files(parsed_options.feed_file, parsed_options.subfeed_file_prefix, int(parsed_options.no_feeders))
    

if __name__ == "__main__":
    main()
