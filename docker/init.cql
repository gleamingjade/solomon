CREATE KEYSPACE IF NOT EXISTS localscylla 
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};