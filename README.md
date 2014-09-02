FISE to FAM converstion Enhancement Engine
==========================================

This module provides a post-processing [Enhancement Engine](http://stanbol.apache.org/docs/trunk/components/enhancer/engines) for [Apache Stanbol](http://stanbol.apache.org) that converts the [Stanbol Enhancement Structure](http://stanbol.apache.org/docs/trunk/components/enhancer/enhancementstructure) (FISE) to the [Fusepool Annotation Model](https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md) (FAM).

For doing so this engine implements the transformation rules as specified [here](https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md#transformation-of-fise-to-the-fusepool-annotation-model)

This engine can be used with Apache Stanbol versions  `>= 0.12.0 < 2`.

### Configuration Parameters

This engine supports an optional configuration. Meaning that the default instance is available with the name `fise2fam`

* __Selector type__ (`enhancer.engine.fise2fam.selectortype.name`): This allows to configure the type of selectors. Supported are NIF 2.0 and Open Annotation. Also a compatibility mode where properties of both are written in supported (default: `NIF`).
    * `NIF`: will activate NIF 2.0 type selectors
    * `OA` : will activate Open Annotation [Text Position Selector](http://www.openannotation.org/spec/core/specific.html#TextPositionSelector) and [Text Quote Selector](http://www.openannotation.org/spec/core/specific.html#TextQuoteSelector).
    * `BOTH`: compatibility mode that will write both NIF and OA selector information
* __Write Metadata__ (`enhancer.engine.fise2fam.metadata.name`): This switch allows to enable/disable the serialization of the metadata. If disabled no `oa:Annotation` and `oa:SpecificResource` will get serialized. Deactivating this option will make the resulting RDF to no longer confirm to the Open Annotation standard. However it will also reduce the triple count by > 50% (default: `enabled`)

### Notes

* This Engine SHOULD BE used in an [Enhancement Chain](http://stanbol.apache.org/docs/trunk/components/enhancer/chains/) after the [TextAnnotation new Model Engine](http://stanbol.apache.org/docs/trunk/components/enhancer/engines/textannotationnewmodel). If not Selectors will not have prefix/suffix information.
