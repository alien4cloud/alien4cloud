curl -X POST "http://localhost:9200/csar/csar/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'


- Query by using curl on elastic search:
curl -X POST "http://localhost:9200/group/_search?pretty=true" -d '
{
    "query" : {
        "bool": {
            "must_not" : {
                "term" : { "name" : "ALL_USERS"}
            }
        }
    }
}
'

curl -X POST "http://localhost:9200/toscaelement/indexedrelationshiptype/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

curl -X DELETE "http://localhost:9200/cloud/_query?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

curl -X DELETE "http://localhost:9200/toscaelement/_query?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

# Recover all applications
curl -X POST "http://localhost:9200/application/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    },
    "post_filter" : {
        "nested" : {
            "path" : "userRoles",
            "filter" : {
                "bool" : {
                    "must" : [
                        {
                            "term" : {"userRoles.key" : "luc"}
                        },
                        {
                            "term" : {"userRoles.value" : "application_manager"}
                        }
                    ]
                }
            },
            "_cache" : true
        }
    }
}
'

curl -X DELETE "http://localhost:9200/application/_query?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

# Csar
curl -X POST "http://localhost:9200/csar/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

curl -X POST "http://localhost:9200/csar/_search?pretty=true" -d '
{
    "query" : {
        "filter" : {
            "term" : { "id" : "csar1:v1-snapshot"}
        }
    }
}
'