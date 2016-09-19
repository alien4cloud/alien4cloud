curl -X POST "http://localhost:9200/csar/csar/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

curl -X POST "http://localhost:9200/paasdeploymentlog/paasdeploymentlog/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

curl -X POST "http://localhost:9200/toscaelement/_search?pretty=true" -d '
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

curl -X POST "http://localhost:9200/deployment/_search?pretty=true" -d '
{
    "query" : {
          "bool" : {
            "must" : [ {
              "term" : {
                "cloudId" : "9b6fd043-7b1f-469e-998f-f647ef0e584d"
              }
            }]
          }
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

curl -X POST "http://localhost:9200/toscaelement/nodetype/_search?pretty=true" -d '
{
    "query" : {
        "bool": {
            "must" : {
                "term" : { "elementId" : "tosca.nodes.compute"}
            }
        }
    }
}
'

curl -X DELETE "http://localhost:9200/deployment/_query?pretty=true" -d '
{
    "query" : {
          "bool" : {
            "must" : [ {
              "term" : {
                "cloudId" : "433d9597-3891-4291-8007-c8bb477b04bb"
              }
            }]
          }
        }
}
'

curl -X DELETE "http://localhost:9200/suggestionentry/_query?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

curl -X POST "http://localhost:9200/suggestionentry/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

# ElasticSearch get queries
## Images
curl -X POST "http://localhost:9200/imagedata/_search?pretty=true" -d '{"from" : 0, "size" : 1000,"query" : {"match_all" : {}}}'

## Applications
curl -X POST "http://localhost:9200/application/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'
curl -X POST "http://localhost:9200/applicationenvironment/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'
curl -X POST "http://localhost:9200/applicationversion/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

## Meta properties
curl -X POST "http://localhost:9200/metapropconfiguration/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

## Components
curl -X POST "http://localhost:9200/csar/_search?pretty=true" -d '{"from" : 0, "size" : 1000,"query" : {"match_all" : {}}}'
curl -X POST "http://localhost:9200/csargitrepository/_search?pretty=true" -d '{"from" : 0, "size" : 1000,"query" : {"match_all" : {}}}'
curl -X POST "http://localhost:9200/toscaelement/_search?pretty=true" -d '{"from" : 0, "size" : 1000,"query" : {"match_all" : {}}}'

### Specific types
curl -X POST "http://localhost:9200/toscaelement/indexedartifacttype/_search?pretty=true" -d '{"from" : 0, "size" : 1000,"query" : {"match_all" : {}}}'
curl -X POST "http://localhost:9200/toscaelement/indexedcapabilitytype/_search?pretty=true" -d '{"from" : 0, "size" : 1000,"query" : {"match_all" : {}}}'
curl -X POST "http://localhost:9200/toscaelement/indexeddatatype/_search?pretty=true" -d '{"from" : 0, "size" : 1000,"query" : {"match_all" : {}}}'
curl -X POST "http://localhost:9200/toscaelement/nodetype/_search?pretty=true" -d '{"from" : 0, "size" : 1000,"query" : {"match_all" : {}}}'
curl -X POST "http://localhost:9200/toscaelement/indexedrelationshiptype/_search?pretty=true" -d '{"from" : 0, "size" : 1000,"query" : {"match_all" : {}}}'

## Deployments
curl -X POST "http://localhost:9200/deployment/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'
curl -X POST "http://localhost:9200/deploymenttopology/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

## Orchestrators
curl -X POST "http://localhost:9200/orchestrator/_search?pretty=true" -d '{"from" : 0, "size" : 100,"query" : {"match_all" : {}}}'
curl -X POST "http://localhost:9200/orchestratorconfiguration/_search?pretty=true" -d '{"from" : 0, "size" : 100,"query" : {"match_all" : {}}}'

## Locations
curl -X POST "http://localhost:9200/location/_search?pretty=true" -d '{"from" : 0, "size" : 100,"query" : {"match_all" : {}}}'
curl -X POST "http://localhost:9200/locationresourcetemplate/_search?pretty=true" -d '{"from" : 0, "size" : 100,"query" : {"match_all" : {}}}'

## Topology templates
curl -X POST "http://localhost:9200/topologytemplate/_search?pretty=true" -d '{"from" : 0, "size" : 100,"query" : {"match_all" : {}}}'
curl -X POST "http://localhost:9200/topologytemplateversion/_search?pretty=true" -d '{"from" : 0, "size" : 100,"query" : {"match_all" : {}}}'

## Topology
curl -X POST "http://localhost:9200/topology/_search?pretty=true" -d '{"from" : 0, "size" : 100,"query" : {"match_all" : {}}}' | grep "ComputeMKV"

## Events

curl -X POST "http://localhost:9200/deploymentmonitorevents/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

## Plugins
curl -X POST "http://localhost:9200/plugin/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'
curl -X POST "http://localhost:9200/pluginconfiguration/_search?pretty=true" -d '
{
    "query" : {
        "match_all" : {}
    }
}
'

curl -X POST "http://localhost:9200/application/_search?pretty=true" -d '
{
        "query" : {
              "bool" : {
                "must" : [ {
                  "term" : {
                    "cloudId" : "433d9597-3891-4291-8007-c8bb477b04bb"
                  }
                }]
              }
            }
}
'
