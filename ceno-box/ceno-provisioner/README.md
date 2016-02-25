# RSS FEEDER provisioner

Recepies to provision multiple rss inserter bridges in ceno network to speed up rss feed insertion.

Copyright (C) 2016 eQuali.ie

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

# How to

- Make a list of the host in ceno_rss_inserter_hosts, they should all have the same name with enumeration starting from 0. like:
    backbone0
    backbone2
    backbone3
    .
    .
    .

- Copy feedlist.txt
- Run the feed rationer with correct prefix for backbone name and number of backbones such as

    ./feed_rationer.py --subfeed-file-prefix=subfeedlist_backbone -n 10


- Move the resulting files

    mv subfeedlist_backbone* roles/rss_feeder/files/

- Provisions the rss inserters

    playbook -i ceno_rss_inserter_hosts rss_feeder


- Collect the node references in

     refs/backbone1
     refs/backbone2
     .
     .
     .

