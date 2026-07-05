-- In MySQL, you can register a source connector first and then create tables afterward.
-- In Scylla, however, the table must be created in advance before registering the source connector.

-- Notice once you deployed server and if you need to create more table with cdc, create table ahead
-- of adding the name of the table in 'scylla.table.names' part in source connector's json config.

CREATE KEYSPACE IF NOT EXISTS localscylla 
WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1};

CREATE TABLE IF NOT EXISTS localscylla.chat_message (
    trial_id UUID,
    id UUID,
    member_id BIGINT,
    content TEXT,
    PRIMARY KEY (trial_id, id)
) WITH cdc = {'enabled': true};