// https://github.com/SolarNetwork/sn-api-core-js Version 0.8.0. Copyright 2017 Matt Magoffin.
(function (global, factory) {
	typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('d3-time-format'), require('crypto-js/enc-base64'), require('crypto-js/enc-hex'), require('crypto-js/hmac-sha256'), require('crypto-js/sha256'), require('uri-js')) :
	typeof define === 'function' && define.amd ? define(['exports', 'd3-time-format', 'crypto-js/enc-base64', 'crypto-js/enc-hex', 'crypto-js/hmac-sha256', 'crypto-js/sha256', 'uri-js'], factory) :
	(factory((global.sn = global.sn || {}),global.d3,global.CryptoJS.Base64,global.CryptoJS.Hex,global.CryptoJS.HmacSHA256,global.CryptoJS.SHA256,global.URI));
}(this, (function (exports,d3TimeFormat,Base64,Hex,HmacSHA256,SHA256,uriJs) { 'use strict';

Base64 = Base64 && 'default' in Base64 ? Base64['default'] : Base64;
Hex = Hex && 'default' in Hex ? Hex['default'] : Hex;
HmacSHA256 = HmacSHA256 && 'default' in HmacSHA256 ? HmacSHA256['default'] : HmacSHA256;
SHA256 = SHA256 && 'default' in SHA256 ? SHA256['default'] : SHA256;

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) {
  return typeof obj;
} : function (obj) {
  return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj;
};











var classCallCheck = function (instance, Constructor) {
  if (!(instance instanceof Constructor)) {
    throw new TypeError("Cannot call a class as a function");
  }
};

var createClass = function () {
  function defineProperties(target, props) {
    for (var i = 0; i < props.length; i++) {
      var descriptor = props[i];
      descriptor.enumerable = descriptor.enumerable || false;
      descriptor.configurable = true;
      if ("value" in descriptor) descriptor.writable = true;
      Object.defineProperty(target, descriptor.key, descriptor);
    }
  }

  return function (Constructor, protoProps, staticProps) {
    if (protoProps) defineProperties(Constructor.prototype, protoProps);
    if (staticProps) defineProperties(Constructor, staticProps);
    return Constructor;
  };
}();







var get = function get(object, property, receiver) {
  if (object === null) object = Function.prototype;
  var desc = Object.getOwnPropertyDescriptor(object, property);

  if (desc === undefined) {
    var parent = Object.getPrototypeOf(object);

    if (parent === null) {
      return undefined;
    } else {
      return get(parent, property, receiver);
    }
  } else if ("value" in desc) {
    return desc.value;
  } else {
    var getter = desc.get;

    if (getter === undefined) {
      return undefined;
    }

    return getter.call(receiver);
  }
};

var inherits = function (subClass, superClass) {
  if (typeof superClass !== "function" && superClass !== null) {
    throw new TypeError("Super expression must either be null or a function, not " + typeof superClass);
  }

  subClass.prototype = Object.create(superClass && superClass.prototype, {
    constructor: {
      value: subClass,
      enumerable: false,
      writable: true,
      configurable: true
    }
  });
  if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass;
};











var possibleConstructorReturn = function (self, call) {
  if (!self) {
    throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
  }

  return call && (typeof call === "object" || typeof call === "function") ? call : self;
};





var slicedToArray = function () {
  function sliceIterator(arr, i) {
    var _arr = [];
    var _n = true;
    var _d = false;
    var _e = undefined;

    try {
      for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) {
        _arr.push(_s.value);

        if (i && _arr.length === i) break;
      }
    } catch (err) {
      _d = true;
      _e = err;
    } finally {
      try {
        if (!_n && _i["return"]) _i["return"]();
      } finally {
        if (_d) throw _e;
      }
    }

    return _arr;
  }

  return function (arr, i) {
    if (Array.isArray(arr)) {
      return arr;
    } else if (Symbol.iterator in Object(arr)) {
      return sliceIterator(arr, i);
    } else {
      throw new TypeError("Invalid attempt to destructure non-iterable instance");
    }
  };
}();

/**
 * An enumerated object base class.
 * 
 * This class is essentially abstract, and must be extended by another
 * class that overrides the {@link module:util~Enum.enumValues} method.
 * 
 * @abstract
 * @alias module:util~Enum
 */
var Enum = function () {

    /**
     * Constructor.
     * 
     * @param {string} name the name
     */
    function Enum(name) {
        classCallCheck(this, Enum);

        this._name = name;
        if (this.constructor === Enum) {
            Object.freeze(this);
        }
    }

    /**
     * Get the enum name.
     * 
     * @returns {string} the  name
     */


    createClass(Enum, [{
        key: "equals",


        /**
         * Test if a string is equal to this enum's name.
         * 
         * As long as enum values are consistently obtained from the {@link module:util~Enum.enumValues}
         * array then enum instances can be compared with `===`. If unsure, this method can be used
         * to compare string values instead.
         * 
         * @param {string} value the value to test
         * @returns {boolean} `true` if `value` is the same as this instance's `name` value 
         */
        value: function equals(value) {
            return value === this.name;
        }

        /**
         * Get all enum values.
         * 
         * This method must be overridden by subclasses to return something meaningful.
         * This implementation returns an empty array.
         * 
         * @abstract
         * @returns {module:util~Enum[]} get all enum values
         */

    }, {
        key: "name",
        get: function get$$1() {
            return this._name;
        }
    }], [{
        key: "enumValues",
        value: function enumValues() {
            return [];
        }

        /**
         * This method takes an array of enums and turns them into a mapped object, using the enum
         * `name` as object property names.
         * 
         * @param {module:util~Enum[]} enums the enum list to turn into a value object
         * @returns {object} an object with enum `name` properties with associated enum values 
         */

    }, {
        key: "enumsValue",
        value: function enumsValue(enums) {
            return Object.freeze(enums.reduce(function (obj, e) {
                obj[e.name] = e;
                return obj;
            }, {}));
        }

        /**
         * Get an enum instance from its name.
         * 
         * This method searches the {@link module:util~Enum#enumVvalues} array for a matching value.
         * 
         * @param {string} name the enum name to get an instnace for
         * @returns {module:util~Enum} the instance, or `undefined` if no instance exists for the given `name`
         */

    }, {
        key: "valueOf",
        value: function valueOf(name) {
            var enums = this.enumValues();
            if (!Array.isArray(enums)) {
                return undefined;
            }
            for (var i = 0, len = enums.length; i < len; i += 1) {
                if (name === enums[i].name) {
                    return enums[i];
                }
            }
        }
    }, {
        key: "namesFor",
        value: function namesFor(set$$1) {
            var result = [];
            if (set$$1) {
                var _iteratorNormalCompletion = true;
                var _didIteratorError = false;
                var _iteratorError = undefined;

                try {
                    for (var _iterator = set$$1[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
                        var e = _step.value;

                        result.push(e.name);
                    }
                } catch (err) {
                    _didIteratorError = true;
                    _iteratorError = err;
                } finally {
                    try {
                        if (!_iteratorNormalCompletion && _iterator.return) {
                            _iterator.return();
                        }
                    } finally {
                        if (_didIteratorError) {
                            throw _iteratorError;
                        }
                    }
                }
            }
            return result;
        }
    }]);
    return Enum;
}();

/**
 * An immutable enum-like object with an associated comparable value.
 *
 * This class is essentially abstract, and must be extended by another
 * class that overrides the inerited {@link module:util~Enum.enumValues} method.
 * 
 * @abstract
 * @extends module:util~Enum
 * @alias module:util~ComparableEnum
 */

var ComparableEnum = function (_Enum) {
    inherits(ComparableEnum, _Enum);

    /**
     * Constructor.
     * 
     * @param {string} name the name
     * @param {number} value the comparable value
     */
    function ComparableEnum(name, value) {
        classCallCheck(this, ComparableEnum);

        var _this = possibleConstructorReturn(this, (ComparableEnum.__proto__ || Object.getPrototypeOf(ComparableEnum)).call(this, name));

        _this._value = value;
        if (_this.constructor === ComparableEnum) {
            Object.freeze(_this);
        }
        return _this;
    }

    /**
     * Get the comparable value.
     * 
     * @returns {number} the value
     */


    createClass(ComparableEnum, [{
        key: 'compareTo',


        /**
         * Compare two ComparableEnum objects based on their <code>value</code> values.
         * 
         * @param {ComparableEnum} other the object to compare to
         * @returns {number} <code>-1</code> if <code>this.value</code> is less than <code>other.value</code>, 
         *                   <code>1</code> if <code>this.value</code> is greater than <code>other.value</code>,
         *                   <code>0</code> otherwise (when the values are equal) 
         */
        value: function compareTo(other) {
            return this.value < other.value ? -1 : this.value > other.value ? 1 : 0;
        }

        /**
         * Compute a complete set of enum values based on a minimum enum and/or set of enums.
         * 
         * If <code>cache</code> is provided, then results computed via <code>minAggregation</code> 
         * will be cached there, and subsequent calls will returned the cached result when appropriate.
         * 
         * @param {ComparableEnum} [minEnum] a minimum enum value
         * @param {Map<string, Set<ComparableEnum>>} [cache] a cache of computed values
         * @returns {Set<ComparableEnum>|null} the computed set, or <code>null</code> if no values match
         */

    }, {
        key: 'value',
        get: function get$$1() {
            return this._value;
        }
    }], [{
        key: 'minimumEnumSet',
        value: function minimumEnumSet(minEnum, cache) {
            if (!minEnum) {
                return null;
            }
            var result = cache ? cache.get(minEnum.name) : undefined;
            if (result) {
                return result;
            }
            result = new Set();
            var _iteratorNormalCompletion = true;
            var _didIteratorError = false;
            var _iteratorError = undefined;

            try {
                for (var _iterator = minEnum.constructor.enumValues()[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
                    var agg = _step.value;

                    if (agg.compareTo(minEnum) > -1) {
                        result.add(agg);
                    }
                }
            } catch (err) {
                _didIteratorError = true;
                _iteratorError = err;
            } finally {
                try {
                    if (!_iteratorNormalCompletion && _iterator.return) {
                        _iterator.return();
                    }
                } finally {
                    if (_didIteratorError) {
                        throw _iteratorError;
                    }
                }
            }

            if (cache) {
                cache.set(minEnum.name, result);
            }
            return result.size > 0 ? result : null;
        }
    }]);
    return ComparableEnum;
}(Enum);

/**
 * A named aggregation.
 * 
 * @extends module:util~ComparableEnum
 * @alias module:domain~Aggregation
 */

var Aggregation = function (_ComparableEnum) {
  inherits(Aggregation, _ComparableEnum);

  /**
      * Constructor.
      * 
      * @param {string} name the unique name for this precision 
      * @param {number} level a relative aggregation level value 
      */
  function Aggregation(name, level) {
    classCallCheck(this, Aggregation);

    var _this = possibleConstructorReturn(this, (Aggregation.__proto__ || Object.getPrototypeOf(Aggregation)).call(this, name, level));

    if (_this.constructor === Aggregation) {
      Object.freeze(_this);
    }
    return _this;
  }

  /**
   * Get the aggregate level value.
  * 
  * This is an alias for {@link module:util~ComparableEnum#value}.
   */


  createClass(Aggregation, [{
    key: 'level',
    get: function get$$1() {
      return this.value;
    }

    /**
     * Get the {@link module:domain~Aggregations} values.
     * 
     * @override
     * @inheritdoc
     */

  }], [{
    key: 'enumValues',
    value: function enumValues() {
      return AggregationValues;
    }
  }]);
  return Aggregation;
}(ComparableEnum);

var AggregationValues = Object.freeze([new Aggregation('Minute', 60), new Aggregation('FiveMinute', 60 * 5), new Aggregation('TenMinute', 60 * 10), new Aggregation('FifteenMinute', 60 * 15), new Aggregation('ThirtyMinute', 60 * 30), new Aggregation('Hour', 3600), new Aggregation('HourOfDay', 3600), new Aggregation('SeasonalHourOfDay', 3600), new Aggregation('Day', 86400), new Aggregation('DayOfWeek', 86400), new Aggregation('SeasonalDayOfWeek', 86400), new Aggregation('Week', 604800), new Aggregation('WeekOfYear', 604800), new Aggregation('Month', 2419200), new Aggregation('RunningTotal', Number.MAX_SAFE_INTEGER)]);

/**
 * The enumeration of supported Aggregation values.
 * 
 * @readonly
 * @enum {module:domain~Aggregation}
 * @property {module:domain~Aggregation} Minute minute
 * @property {module:domain~Aggregation} FiveMinute 5 minutes
 * @property {module:domain~Aggregation} TenMinute 10 minutes
 * @property {module:domain~Aggregation} FifeteenMinute 15 minutes
 * @property {module:domain~Aggregation} ThirtyMinute 30 minutes
 * @property {module:domain~Aggregation} Hour an hour
 * @property {module:domain~Aggregation} HourOfDay an hour of a day, e.g. 1-24
 * @property {module:domain~Aggregation} SeasonalHourOfDay an hour of a day, further grouped into 4 seasons
 * @property {module:domain~Aggregation} Day a day
 * @property {module:domain~Aggregation} DayOfWeek a day of the week, e.g. Monday - Sunday
 * @property {module:domain~Aggregation} SeasonalDayOfWeek a day of the week, further grouped into 4 seasons
 * @property {module:domain~Aggregation} Week a week
 * @property {module:domain~Aggregation} WeekOfYear the week within a year, e.g. 1 - 52
 * @property {module:domain~Aggregation} Month a month
 * @property {module:domain~Aggregation} RunningTotal a complete running total over a time span
 * @alias module:domain~Aggregations
 */
var Aggregations = Aggregation.enumsValue(AggregationValues);

/**
 * An auth token status.
 * 
 * @extends module:util~Enum
 * @alias module:domain~AuthTokenStatus
 */
var AuthTokenStatus = function (_Enum) {
  inherits(AuthTokenStatus, _Enum);

  /**
   * Constructor.
   * 
   * @param {string} name the name
   */
  function AuthTokenStatus(name) {
    classCallCheck(this, AuthTokenStatus);

    var _this = possibleConstructorReturn(this, (AuthTokenStatus.__proto__ || Object.getPrototypeOf(AuthTokenStatus)).call(this, name));

    if (_this.constructor === AuthTokenStatus) {
      Object.freeze(_this);
    }
    return _this;
  }

  /**
  * Get the {@link module:domain~AuthTokenStatuses} values.
  * 
  * @inheritdoc
  */


  createClass(AuthTokenStatus, null, [{
    key: 'enumValues',
    value: function enumValues() {
      return AuthTokenStatusValues;
    }
  }]);
  return AuthTokenStatus;
}(Enum);

var AuthTokenStatusValues = Object.freeze([new AuthTokenStatus('Active'), new AuthTokenStatus('Disabled')]);

/**
 * The enumeration of supported AuthTokenStatus values.
 * 
 * @readonly
 * @enum {module:domain~AuthTokenStatus}
 * @property {module:domain~AuthTokenStatus} Active the token is active and usable
 * @property {module:domain~AuthTokenStatus} Disabled the token is disabled and not usable
 * @alias module:domain~AuthTokenStatuses
 */
var AuthTokenStatuses = AuthTokenStatus.enumsValue(AuthTokenStatusValues);

/**
 * A named auth token type.
 * 
 * @extends module:util~Enum
 * @alias module:domain~AuthTokenType
 */
var AuthTokenType = function (_Enum) {
  inherits(AuthTokenType, _Enum);

  /**
   * Constructor.
   * 
   * @param {string} name the name
   */
  function AuthTokenType(name) {
    classCallCheck(this, AuthTokenType);

    var _this = possibleConstructorReturn(this, (AuthTokenType.__proto__ || Object.getPrototypeOf(AuthTokenType)).call(this, name));

    if (_this.constructor === AuthTokenType) {
      Object.freeze(_this);
    }
    return _this;
  }

  /**
  * Get the {@link AuthTokenTypes} values.
  * 
  * @inheritdoc
  */


  createClass(AuthTokenType, null, [{
    key: 'enumValues',
    value: function enumValues() {
      return AuthTokenTypeValues;
    }
  }]);
  return AuthTokenType;
}(Enum);

var AuthTokenTypeValues = Object.freeze([new AuthTokenType('ReadNodeData'), new AuthTokenType('User')]);

/**
 * The enumeration of supported AuthTokenType values.
 * 
 * @readonly
 * @enum {module:domain~AuthTokenType}
 * @property {module:domain~AuthTokenType} ReadNodeData a read-only token for reading SolarNode data
 * @property {module:domain~AuthTokenType} User full access as the user that owns the token
 * @alias module:domain~AuthTokenTypes
 */
var AuthTokenTypes = AuthTokenType.enumsValue(AuthTokenTypeValues);

/**
 * A pagination criteria object.
 * @alias module:domain~Pagination
 */
var Pagination = function () {

    /**
     * Construct a pagination object.
     * 
     * @param {number} max the maximum number of results to return 
     * @param {number} [offset] the 0-based starting offset 
     */
    function Pagination(max, offset) {
        classCallCheck(this, Pagination);

        this._max = max > 0 ? +max : 0;
        this._offset = offset > 0 ? +offset : 0;
    }

    /**
     * Get the maximum number of results to return.
     * 
     * @returns {number} the maximum number of results
     */


    createClass(Pagination, [{
        key: 'withOffset',


        /**
         * Copy constructor with a new <code>offset</code> value.
         * 
         * @param {number} offset the new offset to use
         * @return {Pagination} a new instance
         */
        value: function withOffset(offset) {
            return new Pagination(this.max, offset);
        }

        /**
         * Get this object as a standard URI encoded (query parameters) string value.
         * 
         * @return {string} the URI encoded string
         */

    }, {
        key: 'toUriEncoding',
        value: function toUriEncoding() {
            var result = '';
            if (this.max > 0) {
                result += 'max=' + this.max;
            }
            if (this.offset > 0) {
                if (result.length > 0) {
                    result += '&';
                }
                result += 'offset=' + this.offset;
            }
            return result;
        }
    }, {
        key: 'max',
        get: function get$$1() {
            return this._max;
        }

        /**
         * Get the results starting offset.
         * 
         * The first available result starts at offset <code>0</code>. Note this is 
         * a raw offset value, not a "page" offset.
         * 
         * @returns {number} the starting result offset
         */

    }, {
        key: 'offset',
        get: function get$$1() {
            return this._offset;
        }
    }]);
    return Pagination;
}();

/**
 * A description of a sort applied to a property of a collection.
 * @alias module:domain~SortDescriptor
 */
var SortDescriptor = function () {

    /**
     * Constructor.
     * 
     * @param {string} key the property to sort on
     * @param {boolean} [descending] `true` to sort in descending order, `false` for ascending
     */
    function SortDescriptor(key, descending) {
        classCallCheck(this, SortDescriptor);

        this._key = key;
        this._descending = !!descending;
    }

    /**
     * Get the sort property name.
     * 
     * @returns {string} the sort key
     */


    createClass(SortDescriptor, [{
        key: 'toUriEncoding',


        /**
         * Get this object as a standard URI encoded (query parameters) string value.
         * 
         * If `index` is provided and non-negative, then the query parameters will
         * be encoded as an array property named `sorts`. Otherwise just
         * bare `key` and `descending` properties will be used. The 
         * `descending` property is only added if it is `true`.
         * 
         * @param {number} [index] an optional array property index
         * @param {string} [propertyName=sorts] an optional array property name, only used if `index` is also provided
         * @return {string} the URI encoded string
         */
        value: function toUriEncoding(index, propertyName) {
            var result = void 0,
                propName = propertyName || 'sorts';
            if (index !== undefined && index >= 0) {
                result = encodeURIComponent(propName + '[' + index + '].key') + '=';
            } else {
                result = 'key=';
            }
            result += encodeURIComponent(this.key);
            if (this.descending) {
                if (index !== undefined && index >= 0) {
                    result += '&' + encodeURIComponent(propName + '[' + index + '].descending') + '=true';
                } else {
                    result += '&descending=true';
                }
            }
            return result;
        }
    }, {
        key: 'key',
        get: function get$$1() {
            return this._key;
        }

        /**
         * Get the sorting direction.
         * 
         * @returns {boolean} `true` if descending order, `false` for ascending
         */

    }, {
        key: 'descending',
        get: function get$$1() {
            return this._descending;
        }
    }]);
    return SortDescriptor;
}();

/**
 * A basic map-like object.
 * 
 * <p>This object includes some utility functions that make it well suited to using
 * as an API query object. For example, the {@link module:util~PropMap#toUriEncoding}
 * method provides a way to serialize this object into URL query parameters.</p>
 * 
 * @alias module:util~PropMap
 */

var PropMap = function () {
    /**
     * Constructor.
     * @param {PropMap|object} props the initial properties; if a `PropMap` instance is provided, the properties
     *                               of that object will be copied into this one; otherwise the object will be
     *                               used directly to hold property values
     */
    function PropMap(props) {
        classCallCheck(this, PropMap);

        /**
         * The object that all properties are stored on. 
         * @member {object} 
         */
        this.props = props instanceof PropMap ? props.properties() : (typeof props === 'undefined' ? 'undefined' : _typeof(props)) === 'object' ? props : {};
    }

    /**
    * Get, set, or remove a property value.
    *
    * @param {string} key the key to get or set the value for
    * @param {*} [newValue] if defined, the new value to set for the given `key`;
    *                       if `null` then the `key` property will be removed
    * @returns {*} if called as a getter, the associated value for the given `key`,
    *              otherwise this object
    */


    createClass(PropMap, [{
        key: 'prop',
        value: function prop(key, newValue) {
            if (arguments.length === 1) {
                return this.props[key];
            }
            if (newValue === null) {
                delete this.props[key];
            } else {
                this.props[key] = newValue;
            }
            return this;
        }

        /**
        * Get, set, or remove multiple properties.
        * 
        * @param {object} [newProps] the new values to set; if any value is `null` that property
         *                            will be deleted
        * @returns {object} if called as a getter, all properties of this object copied into a 
         *                   simple object; otherwise this object
        */

    }, {
        key: 'properties',
        value: function properties(newProps) {
            if (newProps) {
                var _iteratorNormalCompletion = true;
                var _didIteratorError = false;
                var _iteratorError = undefined;

                try {
                    for (var _iterator = Object.keys(newProps)[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
                        var k = _step.value;

                        this.prop(k, newProps[k]);
                    }
                } catch (err) {
                    _didIteratorError = true;
                    _iteratorError = err;
                } finally {
                    try {
                        if (!_iteratorNormalCompletion && _iterator.return) {
                            _iterator.return();
                        }
                    } finally {
                        if (_didIteratorError) {
                            throw _iteratorError;
                        }
                    }
                }

                return this;
            }
            return Object.assign({}, this.props);
        }

        /**
         * Get this object as a standard URI encoded (query parameters) string value.
         * 
         * All enumerable properties of the <code>props</code> property will be added to the
         * result. If any property value is an array, the values of the array will be joined
         * by a comma. Any {@link module:util~Enum} values will have their `name` property used.
         * Any value that has a `toUriEncoding()` function property will have that function
         * invoked, passing the associated property name as the first argument, and the returned
         * value will be used.
         * 
         * @param {string} [propertyName] an optional object property prefix to add to all properties
         * @param {function} [callbackFn] An optional function that will be called for each property.
         *                   The function will be passed property name and value arguments, and must
         *                   return either `null` to skip the property, a 2-element array with the property
         *                   name and value to use, or anything else to use the property as- is.
         * @return {string} the URI encoded string
         */

    }, {
        key: 'toUriEncoding',
        value: function toUriEncoding(propertyName, callbackFn) {
            var result = '';
            var _iteratorNormalCompletion2 = true;
            var _didIteratorError2 = false;
            var _iteratorError2 = undefined;

            try {
                for (var _iterator2 = Object.keys(this.props)[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
                    var k = _step2.value;

                    if (result.length > 0) {
                        result += '&';
                    }
                    var v = this.props[k];
                    if (callbackFn) {
                        var kv = callbackFn(k, v);
                        if (kv === null) {
                            continue;
                        } else if (Array.isArray(kv) && kv.length > 1) {
                            k = kv[0];
                            v = kv[1];
                        }
                    }

                    if (typeof v.toUriEncoding === 'function') {
                        result += v.toUriEncoding(propertyName ? encodeURIComponent(propertyName) + '.' + k : k);
                        continue;
                    }

                    if (propertyName) {
                        result += encodeURIComponent(propertyName) + '.';
                    }
                    result += encodeURIComponent(k) + '=';
                    if (Array.isArray(v)) {
                        v.forEach(function (e, i) {
                            if (i > 0) {
                                result += ',';
                            }
                            if (e instanceof Enum) {
                                e = e.name;
                            }
                            result += encodeURIComponent(e);
                        });
                    } else {
                        if (v instanceof Enum) {
                            v = v.name;
                        }
                        result += encodeURIComponent(v);
                    }
                }
            } catch (err) {
                _didIteratorError2 = true;
                _iteratorError2 = err;
            } finally {
                try {
                    if (!_iteratorNormalCompletion2 && _iterator2.return) {
                        _iterator2.return();
                    }
                } finally {
                    if (_didIteratorError2) {
                        throw _iteratorError2;
                    }
                }
            }

            return result;
        }

        /**
         * Get this object as a standard URI encoded (query parameters) string value with
         * sorting and pagination parameters.
         * 
         * <p>This calls {@link module:util~PropMap#toUriEncoding} first, then encodes 
         * the `sorts` and `pagination` parameters, if provided.
         * 
        * @param {module:domain~SortDescriptor[]} [sorts] optional sort settings to use
        * @param {module:domain~Pagination} [pagination] optional pagination settings to use
         * @param {string} [propertyName] an optional object property prefix to add to all properties
         * @param {function} [callbackFn] An optional function that will be called for each property.
         *                   The function will be passed property name and value arguments, and must
         *                   return either `null` to skip the property, a 2-element array with the property
         *                   name and value to use, or anything else to use the property as- is.
         * @return {string} the URI encoded string
         */

    }, {
        key: 'toUriEncodingWithSorting',
        value: function toUriEncodingWithSorting(sorts, pagination, propertyName, callbackFn) {
            var params = this.toUriEncoding(propertyName, callbackFn);
            if (Array.isArray(sorts)) {
                sorts.forEach(function (sort, i) {
                    if (sort instanceof SortDescriptor) {
                        if (params.length > 0) {
                            params += '&';
                        }
                        params += sort.toUriEncoding(i);
                    }
                });
            }
            if (pagination instanceof Pagination) {
                var paginationParams = pagination.toUriEncoding();
                if (paginationParams) {
                    if (params.length > 0) {
                        params += '&';
                    }
                    params += paginationParams;
                }
            }
            return params;
        }
    }]);
    return PropMap;
}();

var CountryKey = 'country';
var ElevationKey = 'elevation';
var LatitudeKey = 'latitude';
var IdKey = 'id';
var LocalityKey = 'locality';
var LongitudeKey = 'longitude';
var NameKey = 'name';
var PostalCodeKey = 'postalCode';
var RegionKey = 'region';
var StateOrProvinceKey = 'stateOrProvince';
var StreetKey = 'street';
var TimeZoneIdKey = 'timeZoneId';

/**
 * A geographic location.
 * 
 * @extends module:util~PropMap
 * @alias module:domain~Location
 */

var Location = function (_PropMap) {
    inherits(Location, _PropMap);

    /**
    * Constructor.
    * 
    * @param {module:domain~Location|object} loc the location to copy properties from
    */
    function Location(loc) {
        classCallCheck(this, Location);
        return possibleConstructorReturn(this, (Location.__proto__ || Object.getPrototypeOf(Location)).call(this, loc));
    }

    /**
     * A SolarNetwork assigned unique identifier.
     * @type {number}
     */


    createClass(Location, [{
        key: 'id',
        get: function get$$1() {
            return this.prop(IdKey);
        },
        set: function set$$1(val) {
            this.prop(IdKey, val);
        }

        /**
         * A generalized name, can be used for "virtual" locations.
         * @type {string}
         */

    }, {
        key: 'name',
        get: function get$$1() {
            return this.prop(NameKey);
        },
        set: function set$$1(val) {
            this.prop(NameKey, val);
        }

        /**
         * An ISO 3166-1 alpha-2 character country code.
         * @type {string}
         */

    }, {
        key: 'country',
        get: function get$$1() {
            return this.prop(CountryKey);
        },
        set: function set$$1(val) {
            this.prop(CountryKey, val);
        }

        /**
         * A country-specific regional identifier.
         * @type {string}
         */

    }, {
        key: 'region',
        get: function get$$1() {
            return this.prop(RegionKey);
        },
        set: function set$$1(val) {
            this.prop(RegionKey, val);
        }

        /**
         * A country-specific state or province identifier.
         * @type {string}
         */

    }, {
        key: 'stateOrProvince',
        get: function get$$1() {
            return this.prop(StateOrProvinceKey);
        },
        set: function set$$1(val) {
            this.prop(StateOrProvinceKey, val);
        }

        /**
         * Get the locality (city, town).
         * @type {string}
         */

    }, {
        key: 'locality',
        get: function get$$1() {
            return this.prop(LocalityKey);
        },
        set: function set$$1(val) {
            this.prop(LocalityKey, val);
        }

        /**
         * A country-specific postal code.
         * @type {string}
         */

    }, {
        key: 'postalCode',
        get: function get$$1() {
            return this.prop(PostalCodeKey);
        },
        set: function set$$1(val) {
            this.prop(PostalCodeKey, val);
        }

        /**
         * The street address.
         * @type {string}
         */

    }, {
        key: 'street',
        get: function get$$1() {
            return this.prop(StreetKey);
        },
        set: function set$$1(val) {
            this.prop(StreetKey, val);
        }

        /**
         * The decimal world latitude.
         * @type {number}
         */

    }, {
        key: 'latitude',
        get: function get$$1() {
            return this.prop(LatitudeKey);
        },
        set: function set$$1(val) {
            this.prop(LatitudeKey, val);
        }

        /**
         * The decimal world longitude.
         * @type {number}
         */

    }, {
        key: 'longitude',
        get: function get$$1() {
            return this.prop(LongitudeKey);
        },
        set: function set$$1(val) {
            this.prop(LongitudeKey, val);
        }

        /**
         * The elevation above sea level, in meters.
         * @type {number}
         */

    }, {
        key: 'elevation',
        get: function get$$1() {
            return this.prop(ElevationKey);
        },
        set: function set$$1(val) {
            this.prop(ElevationKey, val);
        }

        /**
         * A time zone ID, for example `Pacific/Auckland`.
         * @type {string}
         */

    }, {
        key: 'timeZoneId',
        get: function get$$1() {
            return this.prop(TimeZoneIdKey);
        },
        set: function set$$1(val) {
            this.prop(TimeZoneIdKey, val);
        }
    }]);
    return Location;
}(PropMap);

/**
 * A basic map-like object.
 * 
 * <p>This object includes some utility functions that make it well suited to using
 * as an API query object. For example, the {@link module:util~PropMap#toUriEncoding}
 * method provides a way to serialize this object into URL query parameters.</p>
 * 
 * @alias module:util~PropMap
 */

var PropMap$2 = function () {
    /**
     * Constructor.
     * @param {PropMap|object} props the initial properties; if a `PropMap` instance is provided, the properties
     *                               of that object will be copied into this one; otherwise the object will be
     *                               used directly to hold property values
     */
    function PropMap(props) {
        classCallCheck(this, PropMap);

        /**
         * The object that all properties are stored on. 
         * @member {object} 
         */
        this.props = props instanceof PropMap ? props.properties() : (typeof props === 'undefined' ? 'undefined' : _typeof(props)) === 'object' ? props : {};
    }

    /**
    * Get, set, or remove a property value.
    *
    * @param {string} key the key to get or set the value for
    * @param {*} [newValue] if defined, the new value to set for the given `key`;
    *                       if `null` then the `key` property will be removed
    * @returns {*} if called as a getter, the associated value for the given `key`,
    *              otherwise this object
    */


    createClass(PropMap, [{
        key: 'prop',
        value: function prop(key, newValue) {
            if (arguments.length === 1) {
                return this.props[key];
            }
            if (newValue === null) {
                delete this.props[key];
            } else {
                this.props[key] = newValue;
            }
            return this;
        }

        /**
        * Get, set, or remove multiple properties.
        * 
        * @param {object} [newProps] the new values to set; if any value is `null` that property
         *                            will be deleted
        * @returns {object} if called as a getter, all properties of this object copied into a 
         *                   simple object; otherwise this object
        */

    }, {
        key: 'properties',
        value: function properties(newProps) {
            if (newProps) {
                var _iteratorNormalCompletion = true;
                var _didIteratorError = false;
                var _iteratorError = undefined;

                try {
                    for (var _iterator = Object.keys(newProps)[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
                        var k = _step.value;

                        this.prop(k, newProps[k]);
                    }
                } catch (err) {
                    _didIteratorError = true;
                    _iteratorError = err;
                } finally {
                    try {
                        if (!_iteratorNormalCompletion && _iterator.return) {
                            _iterator.return();
                        }
                    } finally {
                        if (_didIteratorError) {
                            throw _iteratorError;
                        }
                    }
                }

                return this;
            }
            return Object.assign({}, this.props);
        }

        /**
         * Get this object as a standard URI encoded (query parameters) string value.
         * 
         * All enumerable properties of the <code>props</code> property will be added to the
         * result. If any property value is an array, the values of the array will be joined
         * by a comma. Any {@link module:util~Enum} values will have their `name` property used.
         * Any value that has a `toUriEncoding()` function property will have that function
         * invoked, passing the associated property name as the first argument, and the returned
         * value will be used.
         * 
         * @param {string} [propertyName] an optional object property prefix to add to all properties
         * @param {function} [callbackFn] An optional function that will be called for each property.
         *                   The function will be passed property name and value arguments, and must
         *                   return either `null` to skip the property, a 2-element array with the property
         *                   name and value to use, or anything else to use the property as- is.
         * @return {string} the URI encoded string
         */

    }, {
        key: 'toUriEncoding',
        value: function toUriEncoding(propertyName, callbackFn) {
            var result = '';
            var _iteratorNormalCompletion2 = true;
            var _didIteratorError2 = false;
            var _iteratorError2 = undefined;

            try {
                for (var _iterator2 = Object.keys(this.props)[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
                    var k = _step2.value;

                    if (result.length > 0) {
                        result += '&';
                    }
                    var v = this.props[k];
                    if (callbackFn) {
                        var kv = callbackFn(k, v);
                        if (kv === null) {
                            continue;
                        } else if (Array.isArray(kv) && kv.length > 1) {
                            k = kv[0];
                            v = kv[1];
                        }
                    }

                    if (typeof v.toUriEncoding === 'function') {
                        result += v.toUriEncoding(propertyName ? encodeURIComponent(propertyName) + '.' + k : k);
                        continue;
                    }

                    if (propertyName) {
                        result += encodeURIComponent(propertyName) + '.';
                    }
                    result += encodeURIComponent(k) + '=';
                    if (Array.isArray(v)) {
                        v.forEach(function (e, i) {
                            if (i > 0) {
                                result += ',';
                            }
                            if (e instanceof Enum) {
                                e = e.name;
                            }
                            result += encodeURIComponent(e);
                        });
                    } else {
                        if (v instanceof Enum) {
                            v = v.name;
                        }
                        result += encodeURIComponent(v);
                    }
                }
            } catch (err) {
                _didIteratorError2 = true;
                _iteratorError2 = err;
            } finally {
                try {
                    if (!_iteratorNormalCompletion2 && _iterator2.return) {
                        _iterator2.return();
                    }
                } finally {
                    if (_didIteratorError2) {
                        throw _iteratorError2;
                    }
                }
            }

            return result;
        }

        /**
         * Get this object as a standard URI encoded (query parameters) string value with
         * sorting and pagination parameters.
         * 
         * <p>This calls {@link module:util~PropMap#toUriEncoding} first, then encodes 
         * the `sorts` and `pagination` parameters, if provided.
         * 
        * @param {module:domain~SortDescriptor[]} [sorts] optional sort settings to use
        * @param {module:domain~Pagination} [pagination] optional pagination settings to use
         * @param {string} [propertyName] an optional object property prefix to add to all properties
         * @param {function} [callbackFn] An optional function that will be called for each property.
         *                   The function will be passed property name and value arguments, and must
         *                   return either `null` to skip the property, a 2-element array with the property
         *                   name and value to use, or anything else to use the property as- is.
         * @return {string} the URI encoded string
         */

    }, {
        key: 'toUriEncodingWithSorting',
        value: function toUriEncodingWithSorting(sorts, pagination, propertyName, callbackFn) {
            var params = this.toUriEncoding(propertyName, callbackFn);
            if (Array.isArray(sorts)) {
                sorts.forEach(function (sort, i) {
                    if (sort instanceof SortDescriptor) {
                        if (params.length > 0) {
                            params += '&';
                        }
                        params += sort.toUriEncoding(i);
                    }
                });
            }
            if (pagination instanceof Pagination) {
                var paginationParams = pagination.toUriEncoding();
                if (paginationParams) {
                    if (params.length > 0) {
                        params += '&';
                    }
                    params += paginationParams;
                }
            }
            return params;
        }
    }]);
    return PropMap;
}();

/**
 * Format a date into a SolarNet UTC timestamp format.
 * @function
 * @param {Date} date the date to format
 * @returns {string} the formatted date value - `yyyy-MM-dd HH:mm:ss.SSS'Z'`
 * @alias module:format~timestampFormat
 */
var timestampFormat = d3TimeFormat.utcFormat("%Y-%m-%d %H:%M:%S.%LZ");

/**
 * Format a date into a SolarNet UTC date/time format.
 * @function
 * @param {Date} date the date to format
 * @returns {string} the formatted date value - `yyyy-MM-dd HH:mm`
 * @alias module:format~dateTimeFormat
 */
var dateTimeFormat = d3TimeFormat.utcFormat("%Y-%m-%d %H:%M");

/**
 * Format a date into a SolarNet URL UTC date/time format.
 * @function
 * @param {Date} date the date to format
 * @returns {string} the formatted date value - `yyyy-MM-dd'T'HH:mm`
 * @alias module:format~dateTimeUrlFormat
 */
var dateTimeUrlFormat = d3TimeFormat.utcFormat("%Y-%m-%dT%H:%M");

/**
 * Format a date into a SolarNet UTC date format.
 * @function
 * @param {Date} date the date to format
 * @returns {string} the formatted date value - `yyyy-MM-dd`
 * @alias module:format~dateFormat
 */
var dateFormat = d3TimeFormat.utcFormat("%Y-%m-%d");

/**
 * Parse a SolarNet UTC timestamp value.
 * @function
 * @param {string} str the string to parse - `yyyy-MM-dd HH:mm:ss.SSS'Z'
 * @returns {Date} the parsed date, or `null`
 * @alias module:format~timestampParse
 */
var timestampParse = d3TimeFormat.utcParse("%Y-%m-%d %H:%M:%S.%LZ");

/**
 * Parse a SolarNet UTC date/time.
 * @function
 * @param {string} str the string to parse - `yyyy-MM-dd HH:mm
 * @returns {Date} the parsed date, or `null`
 * @alias module:format~dateTimeParse
 */
var dateTimeParse = d3TimeFormat.utcParse("%Y-%m-%d %H:%M");

/**
 * Parse a UTC date string, from a variety of supported formats.
 *
 * @param {String} str the string to parse into a date
 * @returns {Date} the parsed `Date`, or `null` if the date can't be parsed
 * @alias module:format~dateParser
 */
function dateParser(str) {
  var date = d3TimeFormat.isoParse(str) || timestampParse(str) || dateTimeParse(str);
  return date;
}

/**
 * Format a date into an ISO 8601 timestamp or date string, in the UTC time zone.
 * 
 * @param {Date} date the date to format 
 * @param {boolean} [includeTime=false] `true` to format as a timestamp, `false` as just a date
 * @returns {string} the formatted date string
 * @alias module:format~iso8601Date
 */
function iso8601Date(date, includeTime) {
  return '' + date.getUTCFullYear() + (date.getUTCMonth() < 9 ? '0' : '') + (date.getUTCMonth() + 1) + (date.getUTCDate() < 10 ? '0' : '') + date.getUTCDate() + (includeTime ? 'T' + (date.getUTCHours() < 10 ? '0' : '') + date.getUTCHours() + (date.getUTCMinutes() < 10 ? '0' : '') + date.getUTCMinutes() + (date.getUTCSeconds() < 10 ? '0' : '') + date.getUTCSeconds() + 'Z' : '');
}

var AggregationKey = 'aggregation';
var DataPathKey = 'dataPath';
var EndDateKey = 'endDate';
var LocationIdsKey = 'locationIds';
var LocationKey = 'location';
var MetadataFilterKey = 'metadataFilter';
var MostRecentKey = 'mostRecent';
var NodeIdsKey = 'nodeIds';
var QueryKey = 'query';
var SourceIdsKey = 'sourceIds';
var StartDateKey = 'startDate';
var TagsKey = 'tags';
var UserIdsKey = 'userIds';

/**
 * A filter criteria object for datum.
 * 
 * <p>This filter is used to query both node datum and location datum. Not all properties are
 * applicable to both types. Be sure to consult the SolarNet API documentation on the 
 * supported properties for each type.</p>
 * 
 * @extends module:util~PropMap
 * @alias module:domain~DatumFilter
 */

var DatumFilter = function (_PropMap) {
    inherits(DatumFilter, _PropMap);

    /**
     * Constructor.
     * @param {object} [props] initial property values 
     */
    function DatumFilter(props) {
        classCallCheck(this, DatumFilter);
        return possibleConstructorReturn(this, (DatumFilter.__proto__ || Object.getPrototypeOf(DatumFilter)).call(this, props));
    }

    /**
     * A node ID.
     * 
     * This manages the first available node ID from the `nodeIds` property.
     * 
     * @type {number}
     */


    createClass(DatumFilter, [{
        key: 'toUriEncoding',


        /**
         * Get this object as a standard URI encoded (query parameters) string value.
         * 
         * @override
         * @inheritdoc
         */
        value: function toUriEncoding(propertyName, callbackFn) {
            return get(DatumFilter.prototype.__proto__ || Object.getPrototypeOf(DatumFilter.prototype), 'toUriEncoding', this).call(this, propertyName, callbackFn || datumFilterUriEncodingPropertyMapper);
        }
    }, {
        key: 'nodeId',
        get: function get$$1() {
            var nodeIds = this.nodeIds;
            return Array.isArray(nodeIds) && nodeIds.length > 0 ? nodeIds[0] : null;
        },
        set: function set$$1(nodeId) {
            if (nodeId) {
                this.nodeIds = [nodeId];
            } else {
                this.nodeIds = null;
            }
        }

        /**
         * An array of node IDs.
         * @type {number[]}
         */

    }, {
        key: 'nodeIds',
        get: function get$$1() {
            return this.prop(NodeIdsKey);
        },
        set: function set$$1(nodeIds) {
            this.prop(NodeIdsKey, Array.isArray(nodeIds) ? nodeIds : null);
        }

        /**
         * A location ID.
         * 
         * This manages the first available location ID from the `locationIds` property.
         * 
         * @type {number}
         */

    }, {
        key: 'locationId',
        get: function get$$1() {
            var locationIds = this.locationIds;
            return Array.isArray(locationIds) && locationIds.length > 0 ? locationIds[0] : null;
        },
        set: function set$$1(locationId) {
            if (locationId) {
                this.locationIds = [locationId];
            } else {
                this.locationIds = null;
            }
        }

        /**
         * An array of location IDs.
         * @type {number[]}
         */

    }, {
        key: 'locationIds',
        get: function get$$1() {
            return this.prop(LocationIdsKey);
        },
        set: function set$$1(locationIds) {
            this.prop(LocationIdsKey, Array.isArray(locationIds) ? locationIds : null);
        }

        /**
         * A source ID.
         * 
         * This manages the first available source ID from the `sourceIds` property.
         * 
         * @type {string}
         */

    }, {
        key: 'sourceId',
        get: function get$$1() {
            var sourceIds = this.sourceIds;
            return Array.isArray(sourceIds) && sourceIds.length > 0 ? sourceIds[0] : null;
        },
        set: function set$$1(sourceId) {
            if (sourceId) {
                this.sourceIds = [sourceId];
            } else {
                this.sourceIds = null;
            }
        }

        /**
         * An array of source IDs.
         * @type {string[]}
         */

    }, {
        key: 'sourceIds',
        get: function get$$1() {
            return this.prop(SourceIdsKey);
        },
        set: function set$$1(sourceIds) {
            this.prop(SourceIdsKey, Array.isArray(sourceIds) ? sourceIds : null);
        }

        /**
         * A user ID.
         * 
         * This manages the first available location ID from the `userIds` property.
         * 
         * @type {number}
         */

    }, {
        key: 'userId',
        get: function get$$1() {
            var userIds = this.userIds;
            return Array.isArray(userIds) && userIds.length > 0 ? userIds[0] : null;
        },
        set: function set$$1(userId) {
            if (userId) {
                this.userIds = [userId];
            } else {
                this.userIds = null;
            }
        }

        /**
         * An array of user IDs.
         * @type {number[]}
         */

    }, {
        key: 'userIds',
        get: function get$$1() {
            return this.prop(UserIdsKey);
        },
        set: function set$$1(userIds) {
            this.prop(UserIdsKey, Array.isArray(userIds) ? userIds : null);
        }

        /**
         * The "most recent" flag.
         * @type {boolean}
         */

    }, {
        key: 'mostRecent',
        get: function get$$1() {
            return !!this.prop(MostRecentKey);
        },
        set: function set$$1(value) {
            this.prop(MostRecentKey, !!value);
        }

        /**
         * A minimumin date.
         * @type {Date}
         */

    }, {
        key: 'startDate',
        get: function get$$1() {
            return this.prop(StartDateKey);
        },
        set: function set$$1(date) {
            this.prop(StartDateKey, date);
        }

        /**
         * A maximum date.
         * @type {Date}
         */

    }, {
        key: 'endDate',
        get: function get$$1() {
            return this.prop(EndDateKey);
        },
        set: function set$$1(date) {
            this.prop(EndDateKey, date);
        }

        /**
         * A data path, in dot-delimited notation like `i.watts`.
         * @type {string}
         */

    }, {
        key: 'dataPath',
        get: function get$$1() {
            return this.prop(DataPathKey);
        },
        set: function set$$1(path) {
            this.prop(DataPathKey, path);
        }

        /**
         * An aggregation.
         * 
         * Including this in a filter will cause SolarNet to return aggregated results, rather
         * than raw results.
         * 
         * @type {module:domain~Aggregation}
         */

    }, {
        key: 'aggregation',
        get: function get$$1() {
            return this.prop(AggregationKey);
        },
        set: function set$$1(agg) {
            this.prop(AggregationKey, agg instanceof Aggregation ? agg : null);
        }

        /**
         * An array of tags.
         * @type {string[]}
         */

    }, {
        key: 'tags',
        get: function get$$1() {
            return this.prop(TagsKey);
        },
        set: function set$$1(val) {
            this.prop(TagsKey, Array.isArray(val) ? val : null);
        }

        /**
         * A location, used as an example-based search criteria.
         * @type {module:domain~Location}
         */

    }, {
        key: 'location',
        get: function get$$1() {
            return this.prop(LocationKey);
        },
        set: function set$$1(val) {
            this.prop(LocationKey, val instanceof Location ? val : null);
        }

        /**
         * A general full-text style query string.
         * @type {string}
         */

    }, {
        key: 'query',
        get: function get$$1() {
            return this.prop(QueryKey);
        },
        set: function set$$1(val) {
            this.prop(QueryKey, val);
        }

        /**
         * A metadata filter (LDAP style search criteria).
         * @type {string}
         */

    }, {
        key: 'metadataFilter',
        get: function get$$1() {
            return this.prop(MetadataFilterKey);
        },
        set: function set$$1(val) {
            this.prop(MetadataFilterKey, val);
        }
    }]);
    return DatumFilter;
}(PropMap$2);

/**
 * Map DatumFilter properties for URI encoding.
 * 
 * @param {string} key the property key
 * @param {*} value the property value
 * @returns {*} 2-element array for mapped key+value, `null` to skip, or `key` to keep as-is
 * @private
 */


function datumFilterUriEncodingPropertyMapper(key, value) {
    if (key === NodeIdsKey || key === LocationIdsKey || key === SourceIdsKey || key === UserIdsKey) {
        // check for singleton array value, and re-map to singular property by chopping of "s"
        if (Array.isArray(value) && value.length === 1) {
            return [key.substring(0, key.length - 1), value[0]];
        }
    } else if (key === StartDateKey || key === EndDateKey) {
        return [key, dateTimeUrlFormat(value)];
    } else if (key === MostRecentKey && !value) {
        return null;
    }
    return key;
}

/**
 * General metadata with a basic structure.
 * 
 * This metadata can be associated with a variety of objects within SolarNetwork, such
 * as users, nodes, and datum.
 * 
 * @alias module:domain~GeneralMetadata
 */
var GeneralMetadata = function () {

    /**
     * Constructor.
     * 
     * @param {Map<string, *>} [info] the general metadata map
     * @param {Map<string, Map<string, *>>} [propertyInfo] the property metadata map
     * @param {Set<string>} [tags] the tags
     */
    function GeneralMetadata(info, propertyInfo, tags) {
        classCallCheck(this, GeneralMetadata);

        this.info = info || null;
        this.propertyInfo = propertyInfo || null;
        this.tags = tags instanceof Set ? tags : Array.isArray(tags) ? new Set(tags) : null;
    }

    /**
     * Get this object as a standard JSON encoded string value.
     * 
     * @return {string} the JSON encoded string
     */


    createClass(GeneralMetadata, [{
        key: 'toJsonEncoding',
        value: function toJsonEncoding() {
            var result = {};
            var info = this.info;
            if (info) {
                result['m'] = stringMapToObject(info);
            }
            var propertyInfo = this.propertyInfo;
            if (propertyInfo) {
                result['pm'] = stringMapToObject(propertyInfo);
            }
            var tags = this.tags;
            if (tags) {
                result['t'] = Array.from(tags);
            }

            return JSON.stringify(result);
        }

        /**
         * Parse a JSON string into a {@link module:domain~GeneralMetadata} instance.
         * 
         * The JSON must be encoded the same way {@link module:domain~GeneralMetadata#toJsonEncoding} does.
         * 
         * @param {string} json the JSON to parse
         * @returns {module:domain~GeneralMetadata} the metadata instance 
         */

    }], [{
        key: 'fromJsonEncoding',
        value: function fromJsonEncoding(json) {
            var m = void 0,
                pm = void 0,
                t = void 0;
            if (json) {
                var obj = JSON.parse(json);
                m = obj['m'] ? objectToStringMap(obj['m']) : null;
                pm = obj['pm'] ? objectToStringMap(obj['pm']) : null;
                t = Array.isArray(obj['t']) ? new Set(obj['t']) : null;
            }
            return new GeneralMetadata(m, pm, t);
        }
    }]);
    return GeneralMetadata;
}();

/**
 * Convert a `Map` into a simple object.
 * 
 * The keys are assumed to be strings. Values that are themselves `Map` instances
 * will be converted to simple objects as well.
 * 
 * @param {Map<string, *>} strMap a Map with string keys; nested Map objects are also handled
 * @returns {object} a simple object
 * @see {@link objectToStringMap} for the reverse conversion
 * @alias module:domain~stringMapToObject
 */


function stringMapToObject(strMap) {
    var obj = Object.create(null);
    if (strMap) {
        var _iteratorNormalCompletion = true;
        var _didIteratorError = false;
        var _iteratorError = undefined;

        try {
            for (var _iterator = strMap[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
                var _ref = _step.value;

                var _ref2 = slicedToArray(_ref, 2);

                var k = _ref2[0];
                var v = _ref2[1];

                obj[k] = v instanceof Map ? stringMapToObject(v) : v;
            }
        } catch (err) {
            _didIteratorError = true;
            _iteratorError = err;
        } finally {
            try {
                if (!_iteratorNormalCompletion && _iterator.return) {
                    _iterator.return();
                }
            } finally {
                if (_didIteratorError) {
                    throw _iteratorError;
                }
            }
        }
    }
    return obj;
}

/**
 * Convert a simple object into a `Map` instance.
 * 
 * Property values that are themselves objects will be converted into `Map`
 * instances as well.
 * 
 * @param {object} obj a simple object
 * @returns {Map<string, *>} a Map with string keys; nested Map objects are also handled
 * @see {@link module:domain~stringMapToObject} for the reverse conversion
 * @alias module:domain~objectToStringMap
 */
function objectToStringMap(obj) {
    var strMap = new Map();
    if (obj) {
        var _iteratorNormalCompletion2 = true;
        var _didIteratorError2 = false;
        var _iteratorError2 = undefined;

        try {
            for (var _iterator2 = Object.keys(obj)[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
                var k = _step2.value;

                var v = obj[k];
                strMap.set(k, (typeof v === 'undefined' ? 'undefined' : _typeof(v)) === 'object' ? objectToStringMap(v) : v);
            }
        } catch (err) {
            _didIteratorError2 = true;
            _iteratorError2 = err;
        } finally {
            try {
                if (!_iteratorNormalCompletion2 && _iterator2.return) {
                    _iterator2.return();
                }
            } finally {
                if (_didIteratorError2) {
                    throw _iteratorError2;
                }
            }
        }
    }
    return strMap;
}

/**
 * A named instruction state.
 * 
 * @extends module:util~Enum
 * @alias module:domain~InstructionState
 */

var InstructionState = function (_Enum) {
  inherits(InstructionState, _Enum);

  /**
   * Constructor.
   * 
   * @param {string} name the name
   */
  function InstructionState(name) {
    classCallCheck(this, InstructionState);

    var _this = possibleConstructorReturn(this, (InstructionState.__proto__ || Object.getPrototypeOf(InstructionState)).call(this, name));

    if (_this.constructor === InstructionState) {
      Object.freeze(_this);
    }
    return _this;
  }

  /**
  * Get the {@link module:domain~InstructionStates} values.
  * 
  * @inheritdoc
  */


  createClass(InstructionState, null, [{
    key: 'enumValues',
    value: function enumValues() {
      return InstructionStateValues;
    }
  }]);
  return InstructionState;
}(Enum);

var InstructionStateValues = Object.freeze([new InstructionState('Unknown'), new InstructionState('Queued'), new InstructionState('Received'), new InstructionState('Executing'), new InstructionState('Declined'), new InstructionState('Completed')]);

/**
 * The enumeration of supported InstructionState values.
 * 
 * @readonly
 * @enum {module:domain~InstructionState}
 * @property {module:domain~InstructionState} Unknown an unknown state
 * @property {module:domain~InstructionState} Queued the instruction has been received by SolarNet but not yet delivered to its destination
 * @property {module:domain~InstructionState} Received the instruction has been delivered to its destination but not yet acted upon
 * @property {module:domain~InstructionState} Executed the instruction is currently being acted upon
 * @property {module:domain~InstructionState} Declined the destination has declined to execute the instruction, or the execution failed
 * @property {module:domain~InstructionState} Completed the destination has executed successfully
 * @alias module:domain~InstructionStates
 */
var InstructionStates = InstructionState.enumsValue(InstructionStateValues);

/**
 * A location precision object for use with defining named geographic precision.
 * 
 * @extends module:util~ComparableEnum
 * @alias module:domain~LocationPrecision
 */

var LocationPrecision = function (_ComparableEnum) {
  inherits(LocationPrecision, _ComparableEnum);

  /**
   * Constructor.
   * 
   * @param {string} name the unique name for this precision 
   * @param {number} precision a relative precision value for this precision 
   */
  function LocationPrecision(name, precision) {
    classCallCheck(this, LocationPrecision);

    var _this = possibleConstructorReturn(this, (LocationPrecision.__proto__ || Object.getPrototypeOf(LocationPrecision)).call(this, name, precision));

    if (_this.constructor === LocationPrecision) {
      Object.freeze(_this);
    }
    return _this;
  }

  /**
   * Get the relative precision value.
   * 
   * This is an alias for {@link #name}.
   * 
   * @returns {number} the precision
   */


  createClass(LocationPrecision, [{
    key: 'precision',
    get: function get$$1() {
      return this.value;
    }

    /**
     * Get the {@link module:domain~LocationPrecisions} values.
     * 
        * @override
     * @inheritdoc
     */

  }], [{
    key: 'enumValues',
    value: function enumValues() {
      return LocationPrecisionValues;
    }
  }]);
  return LocationPrecision;
}(ComparableEnum);

var LocationPrecisionValues = Object.freeze([new LocationPrecision('LatLong', 1), new LocationPrecision('Block', 5), new LocationPrecision('Street', 10), new LocationPrecision('PostalCode', 20), new LocationPrecision('Locality', 30), new LocationPrecision('StateOrProvince', 40), new LocationPrecision('Region', 50), new LocationPrecision('TimeZone', 60), new LocationPrecision('Country', 70)]);

/**
 * The enumeration of supported LocationPrecision values.
 * 
 * @readonly
 * @enum {module:domain~LocationPrecision}
 * @property {module:domain~LocationPrecision} LatLong GPS coordinates
 * @property {module:domain~LocationPrecision} Block a city block
 * @property {module:domain~LocationPrecision} Street a street
 * @property {module:domain~LocationPrecision} PostalCode a postal code (or "zip code")
 * @property {module:domain~LocationPrecision} Locality a town or city
 * @property {module:domain~LocationPrecision} StateOrProvince a state or province
 * @property {module:domain~LocationPrecision} Region a large region
 * @property {module:domain~LocationPrecision} TimeZone a time zone
 * @property {module:domain~LocationPrecision} Country a country
 * @alias module:domain~LocationPrecisions
 */
var LocationPrecisions = LocationPrecision.enumsValue(LocationPrecisionValues);

/**
 * Get a Set from a Set or array or object, returning `null` if the set would be empty.
 * 
 * @param {Object[]|Set<*>} obj the array, Set, or singleton object to get as a Set
 * @returns {Set<*>} the Set, or `null`
 * @private
 */
function setOrNull(obj) {
	var result = null;
	if (obj instanceof Set) {
		result = obj.size > 0 ? obj : null;
	} else if (Array.isArray(obj)) {
		result = obj.length > 0 ? new Set(obj) : null;
	} else if (obj) {
		result = new Set([obj]);
	}
	return result;
}

/**
 * Merge two sets.
 * 
 * @param {Object[]|Set<*>} [set1] the first set 
 * @param {Object[]|Set<*>} [set2] the second set 
 * @returns {Set<*>} the merged Set, or `null` if neither arguments are sets or 
 *                   neither argument have any values
 * @private
 */
function mergedSets(set1, set2) {
	var s1 = setOrNull(set1);
	var s2 = setOrNull(set2);
	if (s1 === null && s2 === null) {
		return null;
	} else if (s2 === null) {
		return s1;
	} else if (s1 === null) {
		return s2;
	} else {
		var _iteratorNormalCompletion = true;
		var _didIteratorError = false;
		var _iteratorError = undefined;

		try {
			for (var _iterator = s2.values()[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
				var v = _step.value;

				s1.add(v);
			}
		} catch (err) {
			_didIteratorError = true;
			_iteratorError = err;
		} finally {
			try {
				if (!_iteratorNormalCompletion && _iterator.return) {
					_iterator.return();
				}
			} finally {
				if (_didIteratorError) {
					throw _iteratorError;
				}
			}
		}

		return s1;
	}
}

/**
 * An immutable set of security restrictions that can be attached to other objects, like auth tokens.
 * 
 * Use the {@link module:domain~SecurityPolicyBuilder} to create instances of this class with a fluent API.
 * @alias module:domain~SecurityPolicy
 */

var SecurityPolicy = function () {

	/**
  * Constructor.
  * 
  * @param {number[]|Set<number>} [nodeIds] the node IDs to restrict to, or `null` for no restriction
  * @param {string[]|Set<string>} [sourceIds] the source ID to restrict to, or `null` for no restriction
  * @param {module:domain~Aggregation[]|Set<module:domain~Aggregation>} [aggregations] the aggregation names to restrict to, or `null` for no restriction
  * @param {module:domain~Aggregation} [minAggregation] if specified, a minimum aggregation level that is allowed
  * @param {Set<module:domain~LocationPrecision>} [locationPrecisions] the location precision names to restrict to, or `null` for no restriction
  * @param {module:domain~LocationPrecision} [minLocationPrecision] if specified, a minimum location precision that is allowed
  * @param {Set<string>} [nodeMetadataPaths] the `SolarNodeMetadata` paths to restrict to, or `null` for no restriction
  * @param {Set<string>} [userMetadataPaths] the `UserNodeMetadata` paths to restrict to, or `null` for no restriction
  */
	function SecurityPolicy(nodeIds, sourceIds, aggregations, minAggregation, locationPrecisions, minLocationPrecision, nodeMetadataPaths, userMetadataPaths) {
		classCallCheck(this, SecurityPolicy);

		this._nodeIds = setOrNull(nodeIds);
		this._sourceIds = setOrNull(sourceIds);
		this._aggregations = setOrNull(aggregations);
		this._minAggregation = minAggregation instanceof Aggregation ? minAggregation : null;
		this._locationPrecisions = setOrNull(locationPrecisions);
		this._minLocationPrecision = minLocationPrecision instanceof LocationPrecision ? minLocationPrecision : null;
		this._nodeMetadataPaths = setOrNull(nodeMetadataPaths);
		this._userMetadataPaths = setOrNull(userMetadataPaths);
		if (this.constructor === SecurityPolicy) {
			Object.freeze(this);
		}
	}

	/**
  * Get the node IDs.
  * 
  * @returns {Set<number>} the node IDs, or `null`
  */


	createClass(SecurityPolicy, [{
		key: 'toJsonEncoding',


		/**
   * Get this object as a standard JSON encoded string value.
   * 
   * @return {string} the JSON encoded string
   */
		value: function toJsonEncoding() {
			var result = {};
			var val = this.nodeIds;
			if (val) {
				result.nodeIds = Array.from(val);
			}

			val = this.sourceIds;
			if (val) {
				result.sourceIds = Array.from(val);
			}

			val = this.aggregations;
			if (val) {
				result.aggregations = Array.from(val).map(function (e) {
					return e.name;
				});
			}

			val = this.locationPrecisions;
			if (val) {
				result.locationPrecisions = Array.from(val).map(function (e) {
					return e.name;
				});
			}

			val = this.minAggregation;
			if (val) {
				if (result.length > 0) {
					result += '&';
				}
				result.minAggregation = val.name;
			}

			val = this.minLocationPrecision;
			if (val) {
				result.minLocationPrecision = val.name;
			}

			val = this.nodeMetadataPaths;
			if (val) {
				result.nodeMetadataPaths = Array.from(val);
			}

			val = this.userMetadataPaths;
			if (val) {
				result.userMetadataPaths = Array.from(val);
			}

			return JSON.stringify(result);
		}
	}, {
		key: 'nodeIds',
		get: function get$$1() {
			return this._nodeIds;
		}

		/**
   * Get the source IDs.
   * 
   * @returns {Set<string>} the source IDs, or `null`
   */

	}, {
		key: 'sourceIds',
		get: function get$$1() {
			return this._sourceIds;
		}

		/**
   * Get the aggregations.
   * 
   * @returns {Set<module:domain~Aggregation>} the aggregations, or `null`
   */

	}, {
		key: 'aggregations',
		get: function get$$1() {
			return this._aggregations;
		}

		/**
   * Get the location precisions.
   * 
   * @returns {Set<module:domain~LocationPrecision>} the precisions, or `null`
   */

	}, {
		key: 'locationPrecisions',
		get: function get$$1() {
			return this._locationPrecisions;
		}

		/**
   * Get the minimum aggregation.
   * 
   * @returns {module:domain~Aggregation} the minimum aggregation, or `null`
   */

	}, {
		key: 'minAggregation',
		get: function get$$1() {
			return this._minAggregation;
		}

		/**
   * Get the minimum location precision.
   * 
   * @returns {module:domain~LocationPrecision} the minimum precision, or `null`
   */

	}, {
		key: 'minLocationPrecision',
		get: function get$$1() {
			return this._minLocationPrecision;
		}

		/**
   * Get the node metadata paths.
   * 
   * @returns {Set<string>} the node metadata paths, or `null`
   */

	}, {
		key: 'nodeMetadataPaths',
		get: function get$$1() {
			return this._nodeMetadataPaths;
		}

		/**
   * Get the user metadata paths.
   * 
   * @returns {Set<string>} the user metadata paths, or `null`
   */

	}, {
		key: 'userMetadataPaths',
		get: function get$$1() {
			return this._userMetadataPaths;
		}
	}]);
	return SecurityPolicy;
}();

var MIN_AGGREGATION_CACHE = new Map(); // Map<string, Set<Aggregation>>
var MIN_LOCATION_PRECISION_CACHE = new Map(); // Map<string, Set<LocationPrecision>>

/**
 * A mutable builder object for {@link module:domain~SecurityPolicy} instances.
 * @alias module:domain~SecurityPolicyBuilder
 */

var SecurityPolicyBuilder = function () {
	function SecurityPolicyBuilder() {
		classCallCheck(this, SecurityPolicyBuilder);
	}

	createClass(SecurityPolicyBuilder, [{
		key: 'withPolicy',


		/**
   * Apply all properties from another SecurityPolicy.
   * 
   * @param {module:domain~SecurityPolicy} policy the SecurityPolicy to apply
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */
		value: function withPolicy(policy) {
			if (policy) {
				this.withAggregations(policy.aggregations).withMinAggregation(policy.minAggregation).withLocationPrecisions(policy.locationPrecisions).withMinLocationPrecision(policy.minLocationPrecision).withNodeIds(policy.nodeIds).withSourceIds(policy.sourceIds).withNodeMetadataPaths(policy.nodeMetadataPaths).withUserMetadataPaths(policy.userMetadataPaths);
			}
			return this;
		}

		/**
   * Merge all properties from another SecurityPolicy.
   * 
   * @param {module:domain~SecurityPolicy} policy the SecurityPolicy to merge
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'addPolicy',
		value: function addPolicy(policy) {
			if (policy) {
				this.addAggregations(policy.aggregations).addLocationPrecisions(policy.locationPrecisions).addNodeIds(policy.nodeIds).addSourceIds(policy.sourceIds).addNodeMetadataPaths(policy.nodeMetadataPaths).addUserMetadataPaths(policy.userMetadataPaths);
				if (policy.minAggregation) {
					this.withMinAggregation(policy.minAggregation);
				}
				if (policy.minLocationPrecision) {
					this.withMinLocationPrecision(policy.minLocationPrecision);
				}
			}
			return this;
		}

		/**
   * Set the node IDs.
   * 
   * @param {number[]|Set<number>} nodeIds the node IDs to use
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'withNodeIds',
		value: function withNodeIds(nodeIds) {
			this.nodeIds = setOrNull(nodeIds);
			return this;
		}

		/**
   * Add a set of node IDs.
   * 
   * @param {number[]|Set<number>} nodeIds the node IDs to add
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'addNodeIds',
		value: function addNodeIds(nodeIds) {
			return this.withNodeIds(mergedSets(this.nodeIds, nodeIds));
		}

		/**
   * Set the node metadata paths.
   * 
   * @param {string[]|Set<string>} nodeMetadataPaths the path expressions to use
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'withNodeMetadataPaths',
		value: function withNodeMetadataPaths(nodeMetadataPaths) {
			this.nodeMetadataPaths = setOrNull(nodeMetadataPaths);
			return this;
		}

		/**
   * Add a set of node metadata paths.
   * 
   * @param {string[]|Set<string>} nodeMetadataPaths the path expressions to add
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'addNodeMetadataPaths',
		value: function addNodeMetadataPaths(nodeMetadataPaths) {
			return this.withNodeMetadataPaths(mergedSets(this.nodeMetadataPaths, nodeMetadataPaths));
		}

		/**
   * Set the user metadata paths.
   * 
   * @param {string[]|Set<string>} userMetadataPaths the path expressions to use
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'withUserMetadataPaths',
		value: function withUserMetadataPaths(userMetadataPaths) {
			this.userMetadataPaths = setOrNull(userMetadataPaths);
			return this;
		}

		/**
   * Add a set of user metadata paths.
   * 
   * @param {string[]|Set<string>} userMetadataPaths the path expressions to add
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'addUserMetadataPaths',
		value: function addUserMetadataPaths(userMetadataPaths) {
			return this.withUserMetadataPaths(mergedSets(this.userMetadataPaths, userMetadataPaths));
		}

		/**
   * Set the source IDs.
   * 
   * @param {string[]|Set<string>} sourceIds the source IDs to use
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'withSourceIds',
		value: function withSourceIds(sourceIds) {
			this.sourceIds = setOrNull(sourceIds);
			return this;
		}

		/**
   * Add source IDs.
   * 
   * @param {string[]|Set<string>} sourceIds the source IDs to add
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'addSourceIds',
		value: function addSourceIds(sourceIds) {
			return this.withSourceIds(mergedSets(this.sourceIds, sourceIds));
		}

		/**
   * Set the aggregations.
   * 
   * @param {module:domain~Aggregation[]|Set<module:domain~Aggregation>} aggregations the aggregations to use
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'withAggregations',
		value: function withAggregations(aggregations) {
			this.aggregations = setOrNull(aggregations);
			return this;
		}

		/**
   * Set the aggregations.
   * 
   * @param {module:domain~Aggregation[]|Set<module:domain~Aggregation>} aggregations the aggregations to add
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'addAggregations',
		value: function addAggregations(aggregations) {
			return this.withAggregations(mergedSets(this.aggregations, aggregations));
		}

		/**
   * Set the location precisions.
   * 
   * @param {module:domain~LocationPrecision[]|Set<module:domain~LocationPrecision>} locationPrecisions the precisions to use
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'withLocationPrecisions',
		value: function withLocationPrecisions(locationPrecisions) {
			this.locationPrecisions = setOrNull(locationPrecisions);
			return this;
		}

		/**
   * Add location precisions.
   * 
   * @param {module:domain~LocationPrecision[]|Set<module:domain~LocationPrecision>} locationPrecisions the precisions to add
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'addLocationPrecisions',
		value: function addLocationPrecisions(locationPrecisions) {
			return this.withLocationPrecisions(mergedSets(this.locationPrecisions, locationPrecisions));
		}

		/**
   * Set a minimum aggregation level.
   * 
   * @param {module:domain~Aggregation} minAggregation the minimum aggregation level to set
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'withMinAggregation',
		value: function withMinAggregation(minAggregation) {
			this.minAggregation = minAggregation;
			return this;
		}

		/**
   * Build the effective aggregation level set from the policy settings.
   * 
   * This computes a set of aggregation levels based on the configured `minAggregation`
   * and `aggregations` values.
   * 
   * @returns {Set<module:domain~Aggregation>} the aggregation set
   * @private
   */

	}, {
		key: 'buildAggregations',
		value: function buildAggregations() {
			var minAggregation = this.minAggregation;
			var aggregations = this.aggregations;
			if (!minAggregation && aggregations && aggregations.size > 0) {
				return aggregations;
			} else if (!minAggregation) {
				return null;
			}
			return Aggregation.minimumEnumSet(minAggregation, MIN_AGGREGATION_CACHE);
		}

		/**
   * Treat the configured `locationPrecisions` set as a single
   * minimum value or a list of exact values.
   * 
   * By default if `locationPrecisions` is configured with a single
   * value it will be treated as a <em>minimum</em> value, and any
   * {@link module:domain~LocationPrecision} with a {@link module:domain~LocationPrecision#precision} equal 
   * to or higher than that value's level will be included in the generated
   * {@link module:domain~SecurityPolicy#locationPrecisions} set. Set this to
   * `null` to disable that behavior and treat
   * `locationPrecisions` as the exact values to include in the
   * generated {@link module:domain~SecurityPolicy#locationPrecisions} set.
   * 
   * @param {module:domain~LocationPrecision|null} minLocationPrecision
   *        `null` to treat configured location precision values
   *        as-is, or else the minimum threshold
   * @returns {module:domain~SecurityPolicyBuilder} this object
   */

	}, {
		key: 'withMinLocationPrecision',
		value: function withMinLocationPrecision(minLocationPrecision) {
			this.minLocationPrecision = minLocationPrecision;
			return this;
		}

		/**
   * Build the effective aggregation level set from the policy settings.
   * 
   * This computes a set of location precision levels based on the configured `minLocationPrecision`
   * and `locationPrecisions` values.
   * 
   * @returns {Set<module:domain~LocationPrecision>} the precision set
   * @private
   */

	}, {
		key: 'buildLocationPrecisions',
		value: function buildLocationPrecisions() {
			var minLocationPrecision = this.minLocationPrecision;
			var locationPrecisions = this.locationPrecisions;
			if (!minLocationPrecision && locationPrecisions && locationPrecisions.size > 0) {
				return locationPrecisions;
			} else if (!minLocationPrecision) {
				return null;
			}
			return LocationPrecision.minimumEnumSet(minLocationPrecision, MIN_LOCATION_PRECISION_CACHE);
		}

		/**
   * Create a new {@link SecurityPolicy} out of the properties configured on this builder.
   * 
   * @returns {module:domain~SecurityPolicy} the new policy instance
   */

	}, {
		key: 'build',
		value: function build() {
			return new SecurityPolicy(this.nodeIds, this.sourceIds, this.buildAggregations(), this.minAggregation, this.buildLocationPrecisions(), this.minLocationPrecision, this.nodeMetadataPaths, this.userMetadataPaths);
		}
	}]);
	return SecurityPolicyBuilder;
}();

/** @module domain */

/**
 * Get an appropriate multiplier value for scaling a given value to a more display-friendly form.
 * 
 * This will return values suitable for passing to {@link module:format~displayUnitsForScale}.
 * 
 * @param {number} value the value to get a display scale factor for, for example the maximum value
 *                       in a range of values
 * @return {number} the display scale factor
 * @alias module:format~displayScaleForValue
 */
function displayScaleForValue(value) {
  var result = 1,
      num = Math.abs(Number(value));
  if (isNaN(num) === false) {
    if (num >= 1000000000) {
      result = 1000000000;
    } else if (num >= 1000000) {
      result = 1000000;
    } else if (num >= 1000) {
      result = 1000;
    }
  }
  return result;
}

/**
 * Get an appropriate display unit for a given base unit and scale factor.
 * 
 * Use this method to render scaled data value units. Typically you would first call
 * {@link module:module:format~displayScaleForValue}, passing in the largest expected value
 * in a set of data, and then pass the result to this method to generate a display unit
 * for the base unit for that data.
 * 
 * For example, given a base unit of `W` (watts) and a maximum data value of `10000`:
 * 
 * ```
 * const fmt = import { * } from 'format/scale';
 * const displayScale = fmt.displayScaleForValue(10000);
 * const displayUnit = fmt.displayUnitForScale('W', displayScale);
 * ```
 * 
 * The `displayUnit` result in that example would be `kW`.
 *
 * @param {string} baseUnit the base unit, for example `W` or `Wh`
 * @param {number} scale the unit scale, which must be a recognized SI scale, such 
 *                       as `1000` for `k`
 * @return {string} the display unit value
 * @alias module:format~displayUnitsForScale
 */
function displayUnitsForScale(baseUnit, scale) {
  return (scale === 1000000000 ? 'G' : scale === 1000000 ? 'M' : scale === 1000 ? 'k' : '') + baseUnit;
}

/** @module format */

/**
 * A case-insensitive string key multi-value map object.
 * @alias module:util~MultiMap
 */
var MultiMap = function () {

	/**
  * Constructor.
  * 
  * @param {*} [values] an object who's enumerable properties will be added to this map
  */
	function MultiMap(values) {
		classCallCheck(this, MultiMap);

		this.mappings = {}; // map of lower-case header names to {name:X, val:[]} values
		this.mappingNames = []; // to keep insertion order
		if (values) {
			this.putAll(values);
		}
	}

	/**
  * Add a value.
  * 
  * This method will append values to existing keys.
  * 
  * @param {string} key the key to use
  * @param {*} value the value to add
  * @returns {module:util~MutliMap} this object
  */


	createClass(MultiMap, [{
		key: "add",
		value: function add(key, value) {
			return addValue(this, key, value);
		}

		/**
   * Set a value.
   * 
   * This method will replace any existing values with just <code>value</code>.
   * 
   * @param {string} key the key to use
   * @param {*} value the value to set
   * @returns {module:util~MutliMap} this object
   */

	}, {
		key: "put",
		value: function put(key, value) {
			return addValue(this, key, value, true);
		}

		/**
   * Set multiple values.
   * 
   * This method will replace any existing values with those provided on <code>values</code>.
   * 
   * @param {*} values an object who's enumerable properties will be added to this map
   * @returns {module:util~MutliMap} this object
   */

	}, {
		key: "putAll",
		value: function putAll(values) {
			for (var key in values) {
				if (values.hasOwnProperty(key)) {
					addValue(this, key, values[key], true);
				}
			}
			return this;
		}

		/**
   * Get the values associated with a key.
   * 
   * @param {string} key the key of the values to get
   * @returns {object[]} the array of values associated with the key, or <code>undefined</code> if not available
   */

	}, {
		key: "value",
		value: function value(key) {
			var keyLc = key.toLowerCase();
			var mapping = this.mappings[keyLc];
			return mapping ? mapping.val : undefined;
		}

		/**
   * Get the first avaialble value assocaited with a key.
   * 
   * @param {string} key the key of the value to get
   * @returns {*} the first available value associated with the key, or <code>undefined</code> if not available
   */

	}, {
		key: "firstValue",
		value: function firstValue(key) {
			var values = this.value(key);
			return values && values.length > 0 ? values[0] : undefined;
		}

		/**
   * Remove all properties from this map.
   * 
   * @returns {module:util~MutliMap} this object
   */

	}, {
		key: "clear",
		value: function clear() {
			this.mappingNames.length = 0;
			this.mappings = {};
			return this;
		}

		/**
   * Remove all values associated with a key.
   * 
   * @param {string} key the key of the values to remove
   * @returns {object[]} the removed values, or <code>undefined</code> if no values were present for the given key
   */

	}, {
		key: "remove",
		value: function remove(key) {
			var keyLc = key.toLowerCase();
			var index = this.mappingNames.indexOf(keyLc);
			var result = this.mappings[keyLc];
			if (result) {
				delete this.mappings[keyLc];
				this.mappingNames.splice(index, 1);
			}
			return result ? result.val : undefined;
		}

		/**
   * Get the number of entries in this map.
   * 
   * @returns {number} the number of entries in the map
   */

	}, {
		key: "size",
		value: function size() {
			return this.mappingNames.length;
		}

		/**
   * Test if the map is empty.
   * 
   * @returns {boolean} <code>true</code> if there are no entries in this map
   */

	}, {
		key: "isEmpty",
		value: function isEmpty() {
			return this.size() < 1;
		}

		/**
   * Test if there are any values associated with a key.
   * 
   * @param {string} key the key to test
   * @returns {boolean} <code>true</code> if there is at least one value associated with the key
   */

	}, {
		key: "containsKey",
		value: function containsKey(key) {
			return this.value(key) !== undefined;
		}

		/**
   * Get an array of all keys in this map.
   * 
   * @returns {string[]} array of keys in this map, or an empty array if the map is empty
   */

	}, {
		key: "keySet",
		value: function keySet() {
			var result = [];
			var len = this.size();
			for (var i = 0; i < len; i += 1) {
				result.push(this.mappings[this.mappingNames[i]].key);
			}
			return result;
		}
	}]);
	return MultiMap;
}();

/**
 * Add/replace values on a map.
 * 
 * @param {module:util~MutliMap} map the map to mutate 
 * @param {string} key the key to change 
 * @param {*} value the value to add
 * @param {boolean} replace if <code>true</code> then replace all existing values;
 *                          if <code>false</code> append to any existing values
 * @returns {module:util~MutliMap} the passed in <code>map</code>
 * @private
 */


function addValue(map, key, value, replace) {
	var keyLc = key.toLowerCase();
	var mapping = map.mappings[keyLc];
	if (!mapping) {
		mapping = { key: key, val: [] };
		map.mappings[keyLc] = mapping;
		map.mappingNames.push(keyLc);
	}
	if (replace) {
		mapping.val.length = 0;
	}
	if (Array.isArray(value)) {
		var len = value.length;
		for (var i = 0; i < len; i += 1) {
			mapping.val.push(value[i]);
		}
	} else {
		mapping.val.push(value);
	}
	return map;
}

function createGetter(me, prop) {
	return function () {
		return me.map[prop];
	};
}

function createSetter(me, prop) {
	return function (value) {
		me.map[prop] = value;
	};
}

function createProperty(me, prop) {
	Object.defineProperty(me, prop, {
		enumerable: true,
		configurable: true,
		get: createGetter(me, prop),
		set: createSetter(me, prop)
	});
}

/**
 * A configuration utility object.
 *
 * Properties can be get/set by using the {@link module:util~Configuration#value} function.
 * @alias module:util~Configuration
 */

var Configuration = function () {

	/**
  * Constructor.
  *
  * For any properties passed on `initialMap`, {@link module:util~Configuration#value} will
  * be called so those properties are defined on this instance.
  *
  * @param {object} initialMap the optional initial properties to store
  */
	function Configuration(initialMap) {
		classCallCheck(this, Configuration);

		this.map = {};
		if (initialMap !== undefined) {
			this.values(initialMap);
		}
	}

	/**
  * Test if a key is truthy.
  *
  * @param {string} key the key to test
  * @returns {boolean} `true` if the key is enabled
  */


	createClass(Configuration, [{
		key: "enabled",
		value: function enabled(key) {
			if (key === undefined) {
				return false;
			}
			return !!this.map[key];
		}

		/**
   * Set or toggle the enabled status of a given key.
   *
   * <p>If the `enabled` parameter is not passed, then the enabled
   * status will be toggled to its opposite value.</p>
   *
   * @param {string} key they key to set
   * @param {boolean} enabled the optional enabled value to set
   * @returns {module:util~Configuration} this object to allow method chaining
   */

	}, {
		key: "toggle",
		value: function toggle(key, enabled) {
			var val = enabled;
			if (key === undefined) {
				return this;
			}
			if (val === undefined) {
				// in 1-argument mode, toggle current value
				val = this.map[key] === undefined;
			}
			return this.value(key, val === true ? true : null);
		}

		/**
   * Get or set a configuration value.
   *
   * @param {string} key The key to get or set the value for
   * @param {object} [newValue] If defined, the new value to set for the given `key`.
   *                            If `null` then the value will be removed.
   * @returns {object} If called as a getter, the associated value for the given `key`,
   *                   otherwise this object.
   */

	}, {
		key: "value",
		value: function value(key, newValue) {
			if (arguments.length === 1) {
				return this.map[key];
			}
			if (newValue === null) {
				delete this.map[key];
				if (this.hasOwnProperty(key)) {
					delete this[key];
				}
			} else {
				this.map[key] = newValue;
				if (!this.hasOwnProperty(key)) {
					createProperty(this, key);
				}
			}
			return this;
		}

		/**
   * Get or set multiple properties.
   * 
   * @param {object} [newMap] a map of values to set
   * @returns {object} if called as a getter, all properties of this object copied into a simple object;
   *                   otherwise this object
   */

	}, {
		key: "values",
		value: function values(newMap) {
			if (newMap) {
				for (var prop in newMap) {
					if (newMap.hasOwnProperty(prop)) {
						this.value(prop, newMap[prop]);
					}
				}
				return this;
			}
			return Object.assign({}, this.map);
		}
	}]);
	return Configuration;
}();

/**
 * An environment configuration utility object.
 *
 * This extends {@link module:util~Configuration} to add support for standard properties
 * needed to access the SolarNetwork API, such as host and protocol values.
 *
 * @extends module:util~Configuration
 * @alias module:net~Environment
 */

var Environment = function (_Configuration) {
	inherits(Environment, _Configuration);

	/**
  * Constructor.
  *
  * This will define the following default properties, if not supplied on the
  * `config` argument:
  *
  * <dl>
  * <dt>host</dt><dd>`data.solarnetwork.net`</dd>
  * <dt>protocol</dt><dd>`https`</dd>
  * <dt>port</dt><dd>`443`</dd>
  * </dl>
  * 
  * These properties correspond to those on the `window.location` object when
  * running in a browser. Thus to construct an environment based on the location
  * of the current page you can create an instance like this:
  * 
  * ```
  * const env = new Environment(window.location);
  * ```
  *
  * @param {Object} [config] an optional set of properties to start with
  */
	function Environment(config) {
		classCallCheck(this, Environment);
		return possibleConstructorReturn(this, (Environment.__proto__ || Object.getPrototypeOf(Environment)).call(this, Object.assign({
			protocol: 'https',
			host: 'data.solarnetwork.net',
			port: config && config.port ? config.port : config && config.protocol ? config.protocol === 'https' ? 443 : 80 : 443
		}, config)));
	}

	/**
 * Check if TLS is in use via the `https` protocol.
 *
  * @returns {boolean} `true` if the `protocol` is set to `https`
  */


	createClass(Environment, [{
		key: 'useTls',
		value: function useTls() {
			return this.value('protocol') === 'https';
		}
	}]);
	return Environment;
}(Configuration);

var HttpMethod = Object.freeze(
/**
 * Enumeration of HTTP methods (verbs).
 * @enum {string}
 * @alias module:net~HttpMethod
 * @constant
 */
{
	GET: 'GET',
	HEAD: 'HEAD',
	POST: 'POST',
	PUT: 'PUT',
	PATCH: 'PATCH',
	DELETE: 'DELETE',
	OPTIONS: 'OPTIONS',
	TRACE: 'TRACE'
});

var HttpContentType = Object.freeze(
/**
 * Enumeration of common HTTP `Content-Type` values.
 * @enum {string}
 * @alias module:net~HttpContentType
 * @constant
 */
{
	APPLICATION_JSON: 'application/json',
	APPLICATION_JSON_UTF8: 'application/json; charset=UTF-8',
	FORM_URLENCODED: 'application/x-www-form-urlencoded',
	FORM_URLENCODED_UTF8: 'application/x-www-form-urlencoded; charset=UTF-8'
});

/**
 * Support for HTTP headers.
 * 
 * @extends module:util~MultiMap
 * @alias module:net~HttpHeaders
 */

var HttpHeaders = function (_MultiMap) {
	inherits(HttpHeaders, _MultiMap);

	function HttpHeaders() {
		classCallCheck(this, HttpHeaders);
		return possibleConstructorReturn(this, (HttpHeaders.__proto__ || Object.getPrototypeOf(HttpHeaders)).call(this));
	}

	return HttpHeaders;
}(MultiMap);

Object.defineProperties(HttpHeaders, {
	/**
  * The `Authorization` header.
  * 
  * @memberof module:net~HttpHeaders
  * @readonly
  * @type {string}
  */
	'AUTHORIZATION': { value: 'Authorization' },

	/**
  * The `Content-MD5` header.
  * 
  * @memberof module:net~HttpHeaders
  * @readonly
  * @type {string}
  */
	'CONTENT_MD5': { value: 'Content-MD5' },

	/**
  * The `Content-Type` header.
  * 
  * @memberof module:net~HttpHeaders
  * @readonly
  * @type {string}
  */
	'CONTENT_TYPE': { value: 'Content-Type' },

	/**
  * The `Date` header.
  * 
  * @memberof module:net~HttpHeaders
  * @readonly
  * @type {string}
  */
	'DATE': { value: 'Date' },

	/**
  * The `Digest` header.
  * 
  * @memberof module:net~HttpHeaders
  * @readonly
  * @type {string}
  */
	'DIGEST': { value: 'Digest' },

	/**
  * The `Host` header.
  * 
  * @memberof module:net~HttpHeaders
  * @readonly
  * @type {string}
  */
	'HOST': { value: 'Host' },

	/**
  * The `X-SN-Date` header.
  * 
  * @memberof module:net~HttpHeaders
  * @readonly
  * @type {string}
  */
	'X_SN_DATE': { value: 'X-SN-Date' }
});

/**
 * Parse the query portion of a URL string, and return a parameter object for the
 * parsed key/value pairs.
 *
 * <p>Multiple parameters of the same name will be stored as an array on the returned object.</p>
 *
 * @param {string} search the query portion of the URL, which may optionally include
 *                        the leading `?` character
 * @return {object} the parsed query parameters, as a parameter object
 * @alias module:net~urlQueryParse
 */
function urlQueryParse(search) {
    var params = {};
    var pairs;
    var pair;
    var i, len, k, v;
    if (search !== undefined && search.length > 0) {
        // remove any leading ? character
        if (search.match(/^\?/)) {
            search = search.substring(1);
        }
        pairs = search.split('&');
        for (i = 0, len = pairs.length; i < len; i++) {
            pair = pairs[i].split('=', 2);
            if (pair.length === 2) {
                k = decodeURIComponent(pair[0]);
                v = decodeURIComponent(pair[1]);
                if (params[k]) {
                    if (!Array.isArray(params[k])) {
                        params[k] = [params[k]]; // turn into array;
                    }
                    params[k].push(v);
                } else {
                    params[k] = v;
                }
            }
        }
    }
    return params;
}

/**
 * Encode the properties of an object as a URL query string.
 *
 * <p>If an object property has an array value, multiple URL parameters will be encoded for that property.</p>
 *
 * <p>The optional `encoderFn` argument is a function that accepts a string value
 * and should return a URI-safe string for that value.</p>
 *
 * @param {object} parameters an object to encode as URL parameters
 * @param {function} encoderFn an optional function to encode each URI component with;
 *                             if not provided the built-in `encodeURIComponent()` function
 *                             will be used
 * @return {string} the encoded query parameters
 * @alias module:net~urlQueryEncode
 */
function urlQueryEncode(parameters, encoderFn) {
    var result = '',
        prop,
        val,
        i,
        len;
    var encoder = encoderFn || encodeURIComponent;
    function handleValue(k, v) {
        if (result.length) {
            result += '&';
        }
        result += encoder(k) + '=' + encoder(v);
    }
    if (parameters) {
        for (prop in parameters) {
            if (parameters.hasOwnProperty(prop)) {
                val = parameters[prop];
                if (Array.isArray(val)) {
                    for (i = 0, len = val.length; i < len; i++) {
                        handleValue(prop, val[i]);
                    }
                } else {
                    handleValue(prop, val);
                }
            }
        }
    }
    return result;
}

var urlQuery = {
    urlQueryParse: urlQueryParse,
    urlQueryEncode: urlQueryEncode
};

/**
 * The number of milliseconds a signing key is valid for.
 * @type {number}
 * @private
 */
var SIGNING_KEY_VALIDITY = 7 * 24 * 60 * 60 * 1000;

/**
 * A builder object for the SNWS2 HTTP authorization scheme.
 *
 * This builder can be used to calculate a one-off header value, for example:
 *
 * ```
 * let authHeader = new AuthorizationV2Builder("my-token")
 *     .path("/solarquery/api/v1/pub/...")
 *     .build("my-token-secret");
 * ```
 * 
 * Or the builder can be re-used for a given token:
 *
 * ```
 * // create a builder for a token
 * let builder = new AuthorizationV2Builder("my-token");
 *
 * // elsewhere, re-use the builder for repeated requests
 * builder.reset()
 *     .path("/solarquery/api/v1/pub/...")
 *     .build("my-token-secret");
 * ```
 *
 * Additionally, a signing key can be generated and re-used for up to 7 days:
 *
 * ```
 * // create a builder for a token
 * let builder = new AuthorizationV2Builder("my-token")
 *   .saveSigningKey("my-token-secret");
 *
 * // elsewhere, re-use the builder for repeated requests
 * builder.reset()
 *     .path("/solarquery/api/v1/pub/...")
 *     .buildWithSavedKey(); // note previously generated key used
 * ```
 * @alias module:net~AuthorizationV2Builder
 */

var AuthorizationV2Builder = function () {

    /**
     * Constructor.
     * 
     * @param {string} token the auth token to use
     * @param {module:net~Environment} [environment] the environment to use; if not provided a default environment will be created 
     */
    function AuthorizationV2Builder(token, environment) {
        classCallCheck(this, AuthorizationV2Builder);


        /**
         * The SolarNet auth token value.
         * @member {string}
         */
        this.tokenId = token;

        /**
         * The SolarNet environment.
         * @member {module:net~Environment}
         */
        this.environment = environment || new Environment();

        this.reset();
    }

    /**
     * Reset to defalut property values.
     *
     * @returns {module:net~AuthorizationV2Builder} this object
     */


    createClass(AuthorizationV2Builder, [{
        key: 'reset',
        value: function reset() {
            this.contentDigest = null;
            this.httpHeaders = new HttpHeaders();
            this.parameters = new MultiMap();
            this.signedHeaderNames = [];
            var host = this.environment.host;
            if (this.environment.protocol === 'https' || this.environment.port != 80) {
                host += ':' + this.environment.port;
            }
            return this.method(HttpMethod.GET).host(host).path('/').date(new Date());
        }

        /**
         * Compute and cache the signing key.
         *
         * Signing keys are derived from the token secret and valid for 7 days, so
         * this method can be used to compute a signing key so that {@link module:net~AuthorizationV2Builder#build}
         * can be called later. The signing date will be set to whatever date is
         * currently configured via {@link module:net~AuthorizationV2Builder#date}, which defaults to the
         * current time for newly created builder instances.
         *
         * @param {string} tokenSecret the secret to sign the digest with
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'saveSigningKey',
        value: function saveSigningKey(tokenSecret) {
            this.signingKey = this.computeSigningKey(tokenSecret);
            this.signingKeyExpiration = new Date(this.requestDate.getTime() + SIGNING_KEY_VALIDITY);
        }

        /**
         * Test if a signing key is present and not expired.
         * @readonly
         * @type {boolean}
         */

    }, {
        key: 'method',


        /**
         * Set the HTTP method (verb) to use.
         *
         * @param {string} val the method to use; see the {@link HttpMethod} enum for possible values
         * @returns {module:net~AuthorizationV2Builder} this object
         */
        value: function method(val) {
            this.httpMethod = val;
            return this;
        }

        /**
         * Set the HTTP host.
         *
         * This is a shortcut for calling `HttpHeaders#put(HttpHeaders.HOST, val)`.
         *
         * @param {string} val the HTTP host value to use
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'host',
        value: function host(val) {
            this.httpHeaders.put(HttpHeaders.HOST, val);
            return this;
        }

        /**
         * Set the HTTP request path to use.
         *
         * @param {string} val the request path to use
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'path',
        value: function path(val) {
            this.requestPath = val;
            return this;
        }

        /**
         * Set the host, path, and query parameters via a URL string.
         *
         * @param {string} url the URL value to use
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'url',
        value: function url(_url) {
            var uri = uriJs.parse(_url);
            var host = uri.host;
            if (uri.port && ((uri.scheme === 'https' || uri.scheme === 'wss') && uri.port !== 443 || (uri.scheme === 'http' || uri.scheme === 'ws') && uri.port !== 80)) {
                host += ':' + uri.port;
            }
            if (uri.query) {
                this.queryParams(urlQueryParse(uri.query));
            }
            return this.host(host).path(uri.path);
        }

        /**
         * Set the HTTP content type.
         *
         * This is a shortcut for calling {@link HttpHeaders#put} with the key {@link HttpHeaders#CONTENT_TYPE}.
         *
         * @param {string} val the HTTP content type value to use
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'contentType',
        value: function contentType(val) {
            this.httpHeaders.put(HttpHeaders.CONTENT_TYPE, val);
            return this;
        }

        /**
         * Set the authorization request date.
         *
         * @param {Date} val the date to use; typically the current time, e.g. `new Date()`
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'date',
        value: function date(val) {
            this.requestDate = val ? val : new Date();
            return this;
        }

        /**
         * The authorization request date as a HTTP header string value.
         *
         * @readonly
         * @type {string}
         */

    }, {
        key: 'snDate',


        /**
         * Set the `useSnDate` property.
         *
         * @param {boolean} enabled `true` to use the `X-SN-Date` header, `false` to use `Date`
         * @returns {module:net~AuthorizationV2Builder} this object
         */
        value: function snDate(enabled) {
            this.useSnDate = enabled;
            return this;
        }

        /**
         * Set a HTTP header value.
         *
         * This is a shortcut for calling `HttpHeaders#put(headerName, val)`.
         *
         * @param {string} headerName the header name to set
         * @param {string} headerValue the header value to set
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'header',
        value: function header(headerName, headerValue) {
            this.httpHeaders.put(headerName, headerValue);
            return this;
        }

        /**
         * Set the HTTP headers to use with the request.
         *
         * The headers object must include all headers necessary by the
         * authentication scheme, and any additional headers also configured via
         * {@link module:net~AuthorizationV2Builder#signedHttpHeaders}.
         *
         * @param {HttpHeaders} headers the HTTP headers to use
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'headers',
        value: function headers(_headers) {
            this.httpHeaders = _headers;
            return this;
        }

        /**
         * Set the HTTP `GET` query parameters, or `POST` form-encoded
         * parameters.
         *
         * @param {MultiMap|Object} params the parameters to use, as either a {@link MultiMap} or simple `Object`
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'queryParams',
        value: function queryParams(params) {
            if (params instanceof MultiMap) {
                this.parameters = params;
            } else {
                this.parameters.putAll(params);
            }
            return this;
        }

        /**
         * Set additional HTTP header names to sign with the authentication.
         *
         * @param {sring[]} signedHeaderNames additional HTTP header names to include in the signature
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'signedHttpHeaders',
        value: function signedHttpHeaders(signedHeaderNames) {
            this.signedHeaderNames = signedHeaderNames;
            return this;
        }

        /**
         * Set the HTTP request body content SHA-256 digest value.
         *
         * @param {string|module:crypto-js/enc-hex~WordArray} digest the digest value to use; if a string it is assumed to be Hex encoded
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'contentSHA256',
        value: function contentSHA256(digest) {
            var contentDigest;
            if (typeof digest === 'string') {
                contentDigest = Hex.parse(digest);
            } else {
                contentDigest = digest;
            }
            this.contentDigest = contentDigest;
            return this;
        }

        /**
         * Compute the SHA-256 digest of the request body content and configure the result on this builder.
         * 
         * This method will compute the digest and then save the result via the {@link module:net~AuthorizationV2Builder#contentSHA256}
         * method. In addition, it will set the `Digest` HTTP header value via {@link module:net~AuthorizationV2Builder#header}. 
         * This means you <i>must</i> also pass the `Digest` HTTP header with the request. After calling this
         * method, you can retrieve the `Digest` HTTP header value via the `httpHeaders`property.
         * 
         * @param {string} content the request body content to compute a SHA-256 digest value from
         * @returns {module:net~AuthorizationV2Builder} this object
         */

    }, {
        key: 'computeContentDigest',
        value: function computeContentDigest(content) {
            var digest = SHA256(content);
            this.contentSHA256(digest);
            this.header('Digest', 'sha-256=' + Base64.stringify(digest));
            return this;
        }

        /**
         * Compute the canonical query parameters.
         * 
         * @returns {string} the canonical query parameters string value
         */

    }, {
        key: 'canonicalQueryParameters',
        value: function canonicalQueryParameters() {
            var keys = this.parameters.keySet();
            if (keys.length < 1) {
                return '';
            }
            keys.sort();
            var len = keys.length;
            var first = true,
                result = '';
            for (var i = 0; i < len; i += 1) {
                var key = keys[i];
                var vals = this.parameters.value(key);
                var valsLen = vals.length;
                for (var j = 0; j < valsLen; j += 1) {
                    if (first) {
                        first = false;
                    } else {
                        result += '&';
                    }
                    result += _encodeURIComponent(key) + '=' + _encodeURIComponent(vals[j]);
                }
            }
            return result;
        }

        /**
         * Compute the canonical HTTP headers string value.
         * 
         * @param {string[]} sortedLowercaseHeaderNames the sorted, lower-cased HTTP header names to include
         * @returns {string} the canonical headers string value
         */

    }, {
        key: 'canonicalHeaders',
        value: function canonicalHeaders(sortedLowercaseHeaderNames) {
            var result = '',
                headerName,
                headerValue;
            var len = sortedLowercaseHeaderNames.length;
            for (var i = 0; i < len; i += 1) {
                headerName = sortedLowercaseHeaderNames[i];
                if ("date" === headerName || "x-sn-date" === headerName) {
                    headerValue = this.requestDate.toUTCString();
                } else {
                    headerValue = this.httpHeaders.firstValue(headerName);
                }
                result += headerName + ':' + (headerValue ? headerValue.trim() : '') + '\n';
            }
            return result;
        }

        /**
         * Compute the canonical signed header names value from an array of HTTP header names.
         * 
         * @param {string[]} sortedLowercaseHeaderNames the sorted, lower-cased HTTP header names to include
         * @returns {string} the canonical signed header names string value
         * @private
         */

    }, {
        key: 'canonicalSignedHeaderNames',
        value: function canonicalSignedHeaderNames(sortedLowercaseHeaderNames) {
            return sortedLowercaseHeaderNames.join(';');
        }

        /**
         * Get the canonical request content SHA256 digest, hex encoded.
         * 
         * @returns {string} the hex-encoded SHA256 digest of the request content
         */

    }, {
        key: 'canonicalContentSHA256',
        value: function canonicalContentSHA256() {
            return this.contentDigest ? Hex.stringify(this.contentDigest) : AuthorizationV2Builder.EMPTY_STRING_SHA256_HEX;
        }

        /**
         * Compute the canonical HTTP header names to include in the signature.
         * 
         * @returns {string[]} the sorted, lower-cased HTTP header names to include
         */

    }, {
        key: 'canonicalHeaderNames',
        value: function canonicalHeaderNames() {
            var httpHeaders = this.httpHeaders;
            var signedHeaderNames = this.signedHeaderNames;

            // use a MultiMap to take advantage of case-insensitive keys
            var map = new MultiMap();

            map.put(HttpHeaders.HOST, true);
            if (this.useSnDate) {
                map.put(HttpHeaders.X_SN_DATE, true);
            } else {
                map.put(HttpHeaders.DATE, true);
            }
            if (httpHeaders.containsKey(HttpHeaders.CONTENT_MD5)) {
                map.put(HttpHeaders.CONTENT_MD5, true);
            }
            if (httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
                map.put(HttpHeaders.CONTENT_TYPE, true);
            }
            if (httpHeaders.containsKey(HttpHeaders.DIGEST)) {
                map.put(HttpHeaders.DIGEST, true);
            }
            if (signedHeaderNames && signedHeaderNames.length > 0) {
                signedHeaderNames.forEach(function (e) {
                    return map.put(e, true);
                });
            }
            return lowercaseSortedArray(map.keySet());
        }

        /**
         * Compute the canonical request data that will be included in the data to sign with the request.
         * 
         * @returns {string} the canonical request data
         */

    }, {
        key: 'buildCanonicalRequestData',
        value: function buildCanonicalRequestData() {
            return this.computeCanonicalRequestData(this.canonicalHeaderNames());
        }

        /**
         * Compute the canonical request data that will be included in the data to sign with the request,
         * using a specific set of HTTP header names to sign.
         * 
         * @param {string[]} sortedLowercaseHeaderNames the sorted, lower-cased HTTP header names to sign with the request
         * @returns {string} the canonical request data
         * @private
         */

    }, {
        key: 'computeCanonicalRequestData',
        value: function computeCanonicalRequestData(sortedLowercaseHeaderNames) {
            // 1: HTTP verb
            var result = this.httpMethod + '\n';

            // 2: Canonical URI
            result += this.requestPath + '\n';

            // 3: Canonical query string
            result += this.canonicalQueryParameters() + '\n';

            // 4: Canonical headers
            result += this.canonicalHeaders(sortedLowercaseHeaderNames); // already includes newline

            // 5: Signed header names
            result += this.canonicalSignedHeaderNames(sortedLowercaseHeaderNames) + '\n';

            // 6: Content SHA256, hex encoded
            result += this.canonicalContentSHA256();

            return result;
        }

        /**
         * Compute the signing key, from a secret key.
         * 
         * @param {string} secretKey the secret key string 
         * @returns {CryptoJS#Hash} the computed key
         * @private
         */

    }, {
        key: 'computeSigningKey',
        value: function computeSigningKey(secretKey) {
            var datestring = iso8601Date(this.requestDate);
            var key = HmacSHA256('snws2_request', HmacSHA256(datestring, 'SNWS2' + secretKey));
            return key;
        }

        /**
         * Compute the data to be signed by the signing key.
         * 
         * @param {string} canonicalRequestData the request data, returned from {@link module:net~AuthorizationV2Builder#buildCanonicalRequestData}
         * @returns {string} the data to sign
         * @private
         */

    }, {
        key: 'computeSignatureData',
        value: function computeSignatureData(canonicalRequestData) {
            /*- signature data is like:
                 SNWS2-HMAC-SHA256\n
                20170301T120000Z\n
                Hex(SHA256(canonicalRequestData))
            */
            return "SNWS2-HMAC-SHA256\n" + iso8601Date(this.requestDate, true) + "\n" + Hex.stringify(SHA256(canonicalRequestData));
        }

        /**
         * Compute a HTTP `Authorization` header value from the configured properties
         * on the builder, using the provided signing key.
         * 
         * @param {CryptoJS#Hash} signingKey the key to sign the computed signature data with
         * @returns {string} the SNWS2 HTTP Authorization header value
         * @private
         */

    }, {
        key: 'buildWithKey',
        value: function buildWithKey(signingKey) {
            var sortedHeaderNames = this.canonicalHeaderNames();
            var canonicalReq = this.computeCanonicalRequestData(sortedHeaderNames);
            var signatureData = this.computeSignatureData(canonicalReq);
            var signature = Hex.stringify(HmacSHA256(signatureData, signingKey));
            var result = 'SNWS2 Credential=' + this.tokenId + ',SignedHeaders=' + sortedHeaderNames.join(';') + ',Signature=' + signature;
            return result;
        }

        /**
         * Compute a HTTP `Authorization` header value from the configured
         * properties on the builder, computing a new signing key based on the
         * configured {@link module:net~AuthorizationV2Builder#date}.
         *
         * @param {string} tokenSecret the secret to sign the authorization with
         * @return {string} the SNWS2 HTTP Authorization header value
         */

    }, {
        key: 'build',
        value: function build(tokenSecret) {
            var signingKey = this.computeSigningKey(tokenSecret);
            return this.buildWithKey(signingKey);
        }

        /**
         * Compute a HTTP `Authorization` header value from the configured
         * properties on the builder, using a signing key configured from a previous
         * call to {@link module:net~AuthorizationV2Builder#saveSigningKey}.
         *
         * @return {string} the SNWS2 HTTP Authorization header value.
         */

    }, {
        key: 'buildWithSavedKey',
        value: function buildWithSavedKey() {
            return this.buildWithKey(this.signingKey);
        }
    }, {
        key: 'signingKeyValid',
        get: function get$$1() {
            return this.signingKey && this.signingKeyExpiration instanceof Date && Date.now() < this.signingKeyExpiration.getTime() ? true : false;
        }
    }, {
        key: 'requestDateHeaderValue',
        get: function get$$1() {
            return this.requestDate.toUTCString();
        }

        /**
         * Control using the `X-SN-Date` HTTP header versus the `Date` header.
         *
         * <p>Set to `true` to use the `X-SN-Date` header, `false` to use 
         * the `Date` header. This will return `true` if `X-SN-Date` has been
         * added to the `signedHeaderNames` property or has been added to the `httpHeaders`
         * property.</p>
         *
         * @type {boolean}
         */

    }, {
        key: 'useSnDate',
        get: function get$$1() {
            var signedHeaders = this.signedHeaderNames;
            var existingIndex = Array.isArray(signedHeaders) ? signedHeaders.findIndex(caseInsensitiveEqualsFn(HttpHeaders.X_SN_DATE)) : -1;
            return existingIndex >= 0 || this.httpHeaders.containsKey(HttpHeaders.X_SN_DATE);
        },
        set: function set$$1(enabled) {
            var signedHeaders = this.signedHeaderNames;
            var existingIndex = Array.isArray(signedHeaders) ? signedHeaders.findIndex(caseInsensitiveEqualsFn(HttpHeaders.X_SN_DATE)) : -1;
            if (enabled && existingIndex < 0) {
                signedHeaders = signedHeaders ? signedHeaders.concat(HttpHeaders.X_SN_DATE) : [HttpHeaders.X_SN_DATE];
                this.signedHeaderNames = signedHeaders;
            } else if (!enabled && existingIndex >= 0) {
                signedHeaders.splice(existingIndex, 1);
                this.signedHeaderNames = signedHeaders;
            }

            // also clear from httpHeaders
            this.httpHeaders.remove(HttpHeaders.X_SN_DATE);
        }
    }]);
    return AuthorizationV2Builder;
}();

/**
 * @function stringMatchFn
 * @param {string} e the element to test
 * @returns {boolean} `true` if the element matches
 * @private
 */

/**
 * Create a case-insensitive string matching function.
 * 
 * @param {string} value the string to perform the case-insensitive comparison against
 * @returns {stringMatchFn} a matching function that performs a case-insensitive comparison
 * @private
 */


function caseInsensitiveEqualsFn(value) {
    var valueLc = value.toLowerCase();
    return function (e) {
        return valueLc === e.toString().toLowerCase();
    };
}

/**
 * Create a new array of lower-cased and sorted strings from another array.
 * 
 * @param {string[]} items the items to lower-case and sort
 * @returns {string[]} a new array of the lower-cased and sorted items
 * @private
 */
function lowercaseSortedArray(items) {
    var sortedItems = [];
    var len = items.length;
    for (var i = 0; i < len; i += 1) {
        sortedItems.push(items[i].toLowerCase());
    }
    sortedItems.sort();
    return sortedItems;
}

function _hexEscapeChar(c) {
    return '%' + c.charCodeAt(0).toString(16);
}

function _encodeURIComponent(str) {
    return encodeURIComponent(str).replace(/[!'()*]/g, _hexEscapeChar);
}

Object.defineProperties(AuthorizationV2Builder, {
    /**
     * The hex-encoded value for an empty SHA256 digest value.
     * 
     * @memberof AuthorizationV2Builder
     * @readonly
     * @type {string}
     */
    EMPTY_STRING_SHA256_HEX: { value: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855' },

    /**
     * The SolarNetwork V2 authorization scheme.
     * 
     * @memberof AuthorizationV2Builder
     * @readonly
     * @type {string}
     */
    SNWS2_AUTH_SCHEME: { value: 'SNWS2' }
});

/**
 * A utility class for helping to compose SolarNet URLs for the REST API.
 *
 * This class is essentially abstract and meant to have mixin helper objects extend it.
 * @alias module:net~UrlHelper
 */

var UrlHelper = function () {

    /**
     * Constructor.
     *
     * @param {module:net~Environment|object} [environment] the optional initial environment to use;
     *        if a non-`Environment` object is passed then the properties of that object will
     *        be used to construct a new `Environment` instance
     */
    function UrlHelper(environment) {
        classCallCheck(this, UrlHelper);

        var env = environment instanceof Environment ? environment : new Environment(environment);

        /**
         * The environment associated with this helper.
         * @member {module:net~Environment}
         */
        this.environment = env;

        this._parameters = new Configuration();
    }

    /**
     * Get a parameters object that can be used to hold URL variables.
     * 
     * @readonly
     * @type {module:util~Configuration}
     */


    createClass(UrlHelper, [{
        key: 'env',


        /**
         * Get or set an environment parameter.
         * 
         * This is a shortcut for calling {@link module:net~Configuration#value} on the
         * `environment` object.
         * 
         * @param {string} key the environment parameter name to get
         * @param {object} [val] the optional value to set
         * @returns {object} when called as a getter, the environment parameter value;
         *                   when called as a setter, the environment parameters object
         */
        value: function env() {
            var _environment;

            return (_environment = this.environment).value.apply(_environment, arguments);
        }

        /**
         * Get or set a parameter.
         * 
         * This is a shortcut for calling {@link module:net~Configuration#value} on the
         * `parameters` object.
         * 
         * @param {string} key the parameter name to get
         * @param {Object} [val] the optional value to set
         * @returns {Object} when called as a getter, the parameter value;
         *                   when called as a setter, the parameters object
         */

    }, {
        key: 'parameter',
        value: function parameter() {
            var _parameters;

            return (_parameters = this._parameters).value.apply(_parameters, arguments);
        }

        /**
         * Get a URL for just the SolarNet host, without any path.
         *
         * @returns {string} the URL to the SolarNet host
         */

    }, {
        key: 'hostUrl',
        value: function hostUrl() {
            var tls = this.environment.useTls();
            var port = +this.environment.value('port');
            var url = 'http' + (tls ? 's' : '') + '://' + this.environment.value('host');
            if (tls && port > 0 && port !== 443 || !tls && port > 0 && port !== 80) {
                url += ':' + port;
            }
            return url;
        }

        /**
         * Get a URL for just the SolarNet host using the WebSocket protocol, without any path.
         * 
         * @returns {string} the URL to the SolarNet host WebSocket
         */

    }, {
        key: 'hostWebSocketUrl',
        value: function hostWebSocketUrl() {
            var tls = this.environment.useTls();
            var port = +this.environment.value('port');
            var url = 'ws' + (tls ? 's' : '') + '://' + this.environment.value('host');
            if (tls && port > 0 && port !== 443 || !tls && port > 0 && port !== 80) {
                url += ':' + port;
            }
            return url;
        }

        /**
         * Get the base URL to the REST API.
         * 
         * This implementation is a stub, meant for subclasses to override. This implementation
            * simply returns {@link module:net~UrlHelper#hostUrl}.
         * 
            * @abstract
         * @returns {string} the base URL to the REST API
         */

    }, {
        key: 'baseUrl',
        value: function baseUrl() {
            return this.hostUrl();
        }

        /**
         * Replace occurances of URL template variables with values from the `parameters`
         * property and append to the host URL.
         * 
         * This method provides a way to resolve an absolute URL based on the configured
         * environment and parameters on this object.
         * 
         * @param {string} template a URL path template
         * @returns {string} an absolute URL
         * @see module:net~UrlHelper#resolveTemplateUrl
         */

    }, {
        key: 'resolveTemplatePath',
        value: function resolveTemplatePath(template) {
            return this.hostUrl() + this.resolveTemplateUrl(template);
        }

        /**
        * Replace occurances of URL template variables with values from the `parameters`
        * property.
        * 
        * URL template variables are specified as `{<em>name</em>}`. The variable
        * will be replaced by the value associated with property `name` in the
        * `parameters` object. The value will be URI encoded.
        * 
        * @param {string} template a URL template
        * @returns {string} the URL with template variables resolved
        */

    }, {
        key: 'resolveTemplateUrl',
        value: function resolveTemplateUrl(template) {
            return UrlHelper.resolveTemplateUrl(template, this._parameters);
        }

        /**
         * Replace occurances of URL template variables with values from a parameter object.
         * 
         * URL template variables are specified as `{<em>name</em>}`. The variable
         * will be replaced by the value associated with property `name` in the
         * provided parameter object. The value will be URI encoded.
         * 
         * @param {string} template a URL template
         * @param {object} params an object whose properties should serve as template variables
         * @returns {string} the URL
         */

    }, {
        key: 'parameters',
        get: function get$$1() {
            return this._parameters;
        }
    }], [{
        key: 'resolveTemplateUrl',
        value: function resolveTemplateUrl(template, params) {
            return template.replace(/\{([^}]+)\}/g, function (match, variableName) {
                var variableValue = params[variableName];
                return variableValue !== undefined ? encodeURIComponent(variableValue) : '';
            });
        }
    }]);
    return UrlHelper;
}();

var LocationIdsKey$1 = 'locationIds';
var SourceIdsKey$1 = 'sourceIds';

/**
 * Create a LocationUrlHelperMixin class.
 *
 * @param {module:net~UrlHelper} superclass the UrlHelper class to mix onto
 * @return {module:net~LocationUrlHelperMixin} the mixin class
 */
var LocationUrlHelperMixin = function LocationUrlHelperMixin(superclass) {
    return (

        /**
         * A mixin class that adds support for SolarLocation properties to a {@link module:net~UrlHelper}.
         * 
         * @mixin
         * @alias module:net~LocationUrlHelperMixin
         */
        function (_superclass) {
            inherits(_class, _superclass);

            function _class() {
                classCallCheck(this, _class);
                return possibleConstructorReturn(this, (_class.__proto__ || Object.getPrototypeOf(_class)).apply(this, arguments));
            }

            createClass(_class, [{
                key: 'findLocationsUrl',


                /**
                 * Generate a URL to find locations based on a search criteria.
                 * 
                 * @param {module:domain~Location} filter the search criteria
                * @param {module:domain~SortDescriptor[]} [sorts] optional sort settings to use
                * @param {module:domain~Pagination} [pagination] optional pagination settings to use
                 * @returns {string} the generated URL
                 */
                value: function findLocationsUrl(filter, sorts, pagination) {
                    return this.baseUrl() + '/location?' + filter.toUriEncodingWithSorting(sorts, pagination);
                }
            }, {
                key: 'locationId',


                /**
                 * The first available location ID from the `locationIds` property.
                 * Setting this replaces any existing location IDs with an array of just that value.
                 * @type {number}
                 */
                get: function get$$1() {
                    var locationIds = this.locationIds;
                    return Array.isArray(locationIds) && locationIds.length > 0 ? locationIds[0] : null;
                },
                set: function set$$1(locationId) {
                    this.parameter(LocationIdsKey$1, locationId ? [locationId] : null);
                }

                /**
                 * An array of location IDs, set on the `locationIds` parameter
                 * @type {number[]}
                 */

            }, {
                key: 'locationIds',
                get: function get$$1() {
                    return this.parameter(LocationIdsKey$1);
                },
                set: function set$$1(locationIds) {
                    this.parameter(LocationIdsKey$1, locationIds);
                }

                /**
                 * The first available source ID from the `sourceIds` property.
                 * Setting this replaces any existing location IDs with an array of just that value.
                 * @type {string}
                 */

            }, {
                key: 'sourceId',
                get: function get$$1() {
                    var sourceIds = this.sourceIds;
                    return Array.isArray(sourceIds) && sourceIds.length > 0 ? sourceIds[0] : null;
                },
                set: function set$$1(sourceId) {
                    this.parameter(SourceIdsKey$1, sourceId ? [sourceId] : sourceId);
                }

                /**
                 * An array of source IDs, set on the `sourceIds` parameter
                 * @type {string[]}
                 */

            }, {
                key: 'sourceIds',
                get: function get$$1() {
                    return this.parameter(SourceIdsKey$1);
                },
                set: function set$$1(sourceIds) {
                    this.parameter(SourceIdsKey$1, sourceIds);
                }
            }]);
            return _class;
        }(superclass)
    );
};

/** 
 * The SolarQuery default path.
 * @type {string}
 * @alias module:net~SolarQueryDefaultPath
 */
var SolarQueryDefaultPath = '/solarquery';

/** 
 * The {@link module:net~UrlHelper#parameters} key for the SolarQuery path.
 * @type {string}
 * @alias module:net~SolarQueryPathKey
 */
var SolarQueryPathKey = 'solarQueryPath';

/** 
 * The SolarQuery REST API path.
 * @type {string}
 * @alias module:net~SolarQueryApiPathV1
 */
var SolarQueryApiPathV1 = '/api/v1';

/** 
 * The {@link module:net~UrlHelper#parameters} key that holds a `boolean` flag to
 * use the public path scheme (`/pub`) when constructing URLs.
 * @type {string}
 * @alias module:net~SolarQueryPublicPathKey
 */
var SolarQueryPublicPathKey = 'publicQuery';

/**
 * Create a QueryUrlHelperMixin class.
 *
 * @param {module:net~UrlHelper} superclass the UrlHelper class to mix onto
 * @return {module:net~QueryUrlHelperMixin} the mixin class
 */
var QueryUrlHelperMixin = function QueryUrlHelperMixin(superclass) {
  return (

    /**
     * A mixin class that adds SolarQuery specific support to {@link module:net~UrlHelper}.
     * 
     * @mixin
     * @alias module:net~QueryUrlHelperMixin
     */
    function (_superclass) {
      inherits(_class, _superclass);

      function _class() {
        classCallCheck(this, _class);
        return possibleConstructorReturn(this, (_class.__proto__ || Object.getPrototypeOf(_class)).apply(this, arguments));
      }

      createClass(_class, [{
        key: 'baseUrl',


        /**
         * Get the base URL to the SolarQuery v1 REST API.
         * 
         * The returned URL uses the configured environment to resolve
         * the `hostUrl`, the `solarQueryPath` context path,
            * and the `publicQuery` boolean flag. If the context path is not 
            * available, it will default to `/solarquery`.
         * 
         * @returns {string} the base URL to SolarQuery
         */
        value: function baseUrl() {
          var path = this.env(SolarQueryPathKey) || SolarQueryDefaultPath;
          var isPubPath = this.publicQuery;
          return this.hostUrl() + path + SolarQueryApiPathV1 + (isPubPath ? '/pub' : '/sec');
        }
      }, {
        key: 'publicQuery',


        /**
         * Flag to set the `publicQuery` environment parameter.
         * @type {boolean}
         */
        get: function get$$1() {
          return !!this.env(SolarQueryPublicPathKey);
        },
        set: function set$$1(value) {
          this.env(SolarQueryPublicPathKey, !!value);
        }
      }]);
      return _class;
    }(superclass)
  );
};

/**
 * Create a LocationDatumMetadataUrlHelperMixin class.
 *
 * @param {module:net~UrlHelper} superclass the UrlHelper class to mix onto
 * @return {module:net~LocationDatumMetadataUrlHelperMixin} the mixin class
 */
var LocationDatumMetadataUrlHelperMixin = function LocationDatumMetadataUrlHelperMixin(superclass) {
    return (

        /**
         * A mixin class that adds SolarNode datum metadata support to {@link module:net~UrlHelper}.
         * 
         * <p>Location datum metadata is metadata associated with a specific location and source, i.e. 
         * a `locationId` and a `sourceId`.
         * 
         * @mixin
         * @alias module:net~LocationDatumMetadataUrlHelperMixin
         */
        function (_superclass) {
            inherits(_class, _superclass);

            function _class() {
                classCallCheck(this, _class);
                return possibleConstructorReturn(this, (_class.__proto__ || Object.getPrototypeOf(_class)).apply(this, arguments));
            }

            createClass(_class, [{
                key: 'baseLocationDatumMetadataUrl',


                /**
                 * Get a base URL for location datum metadata operations using a specific location ID.
                 * 
                 * @param {number} [locationId] a specific location ID to use; if not provided the `locationId` property of this class will be used
                 * @returns {string} the base URL
                 * @private
                 */
                value: function baseLocationDatumMetadataUrl(locationId) {
                    return this.baseUrl() + '/location/meta/' + (locationId || this.locationId);
                }
            }, {
                key: 'locationDatumMetadataUrlWithSource',
                value: function locationDatumMetadataUrlWithSource(locationId, sourceId) {
                    var result = this.baseLocationDatumMetadataUrl(locationId);
                    var source = sourceId || this.sourceId;
                    if (sourceId !== null && source) {
                        result += '?sourceId=' + encodeURIComponent(source);
                    }
                    return result;
                }

                /**
                 * Generate a URL for viewing datum metadata.
                    * 
                    * If no `sourceId` is provided, then the API will return all available datum metadata for all sources.
                 *
                 * @param {number} [locationId] a specific location ID to use; if not provided the `locationId` property of this class will be used
                 * @param {string} [sourceId] a specific source ID to use; 
                    *                            if not provided the `sourceId` property of this class will be used;
                    *                            if `null` then ignore any `sourceId` property of this class
                    * @returns {string} the URL
                 */

            }, {
                key: 'viewLocationDatumMetadataUrl',
                value: function viewLocationDatumMetadataUrl(locationId, sourceId) {
                    return this.locationDatumMetadataUrlWithSource(locationId, sourceId);
                }

                /**
                 * Generate a URL for adding (merging) datum metadata via a `POST` request.
                    * 
                 * @param {number} [locationId] a specific location ID to use; if not provided the `locationId` property of this class will be used
                 * @param {string} [sourceId] a specific source ID to use; if not provided the `sourceId` property of this class will be used
                    * @returns {string} the URL
                 */

            }, {
                key: 'addLocationDatumMetadataUrl',
                value: function addLocationDatumMetadataUrl(locationId, sourceId) {
                    return this.locationDatumMetadataUrlWithSource(locationId, sourceId);
                }

                /**
                 * Generate a URL for setting datum metadata via a `PUT` request.
                    * 
                 * @param {number} [locationId] a specific location ID to use; if not provided the `locationId` property of this class will be used
                 * @param {string} [sourceId] a specific source ID to use; if not provided the `sourceId` property of this class will be used
                    * @returns {string} the URL
                 */

            }, {
                key: 'replaceLocationDatumMetadataUrl',
                value: function replaceLocationDatumMetadataUrl(locationId, sourceId) {
                    return this.locationDatumMetadataUrlWithSource(locationId, sourceId);
                }

                /**
                 * Generate a URL for deleting datum metadata via a `DELETE` request.
                    * 
                 * @param {number} [locationId] a specific location ID to use; if not provided the `locationId` property of this class will be used
                 * @param {string} [sourceId] a specific source ID to use; if not provided the `sourceId` property of this class will be used
                    * @returns {string} the URL
                 */

            }, {
                key: 'deleteLocationDatumMetadataUrl',
                value: function deleteLocationDatumMetadataUrl(locationId, sourceId) {
                    return this.locationDatumMetadataUrlWithSource(locationId, sourceId);
                }

                /**
                 * Generate a URL for searching for location metadata.
                 * 
                    * @param {module:domain~DatumFilter} [filter] a search filter; the `locationIds`, `sourceIds`, `tags`,
                 *                                    `query`, and `location` properties are supported 
                 * @param {module:domain~SortDescriptor[]} [sorts] optional sort settings to use
                 * @param {module:domain~Pagination} [pagination] optional pagination settings to use
                 * @returns {string} the URL
                 */

            }, {
                key: 'findLocationDatumMetadataUrl',
                value: function findLocationDatumMetadataUrl(filter, sorts, pagination) {
                    var result = this.baseUrl() + '/location/meta';
                    var params = filter.toUriEncodingWithSorting(sorts, pagination);
                    if (params.length > 0) {
                        result += '?' + params;
                    }
                    return result;
                }
            }]);
            return _class;
        }(superclass)
    );
};

/**
 * A concrete {@link module:net~UrlHelper} with the {@link module:net~LocationDatumMetadataUrlHelperMixin},  
 * {@link module:net~QueryUrlHelperMixin}, and {@link module:net~LocationUrlHelperMixin} mixins.
 * 
 * @mixes module:net~LocationDatumMetadataUrlHelperMixin
 * @mixes module:net~QueryUrlHelperMixin
 * @mixes module:net~LocationUrlHelperMixin
 * @extends module:net~UrlHelper
 * @alias module:net~LocationDatumMetadataUrlHelper
 */

var LocationDatumMetadataUrlHelper = function (_LocationDatumMetadat) {
    inherits(LocationDatumMetadataUrlHelper, _LocationDatumMetadat);

    function LocationDatumMetadataUrlHelper() {
        classCallCheck(this, LocationDatumMetadataUrlHelper);
        return possibleConstructorReturn(this, (LocationDatumMetadataUrlHelper.__proto__ || Object.getPrototypeOf(LocationDatumMetadataUrlHelper)).apply(this, arguments));
    }

    return LocationDatumMetadataUrlHelper;
}(LocationDatumMetadataUrlHelperMixin(QueryUrlHelperMixin(LocationUrlHelperMixin(UrlHelper))));

/**
 * Create a LocationDatumUrlHelperMixin class.
 *
 * @param {module:net~UrlHelper} superclass the UrlHelper class to mix onto
 * @return {module:net~LocationDatumUrlHelperMixin} the mixin class
 */
var LocationDatumUrlHelperMixin = function LocationDatumUrlHelperMixin(superclass) {
	return (

		/**
   * A mixin class that adds SolarLocation datum query support to {@link module:net~UrlHelper}.
   * 
   * <p>This mixin is commonly mixed with the {@link module:net~QueryUrlHelperMixin} to pick
   * up support for the SolarQuery base URL.</p>
   * 
   * <p>This mixin is commonly mixed with the {@link module:net~LocationUrlHelperMixin} to
   * pick up support for `locationId` and `sourceId` properties.</p>
   * 
   * @mixin
   * @alias module:net~LocationDatumUrlHelperMixin
   */
		function (_superclass) {
			inherits(_class, _superclass);

			function _class() {
				classCallCheck(this, _class);
				return possibleConstructorReturn(this, (_class.__proto__ || Object.getPrototypeOf(_class)).apply(this, arguments));
			}

			createClass(_class, [{
				key: 'reportableIntervalUrl',


				/**
     * Generate a URL for the "reportable interval" for a location, optionally limited to a specific source ID.
     *
        * If no source IDs are provided, then the reportable interval query will return an interval for
        * all available sources.
        *
     * @param {number} [locationId] a specific location ID to use; if not provided the `locationId` property of this class will be used
     * @param {string} [sourceId] a specific source ID to limit query to; 
        *                 if not provided the `sourceId` property of this class will be used;
        *                 if `null` the `sourceId` property of this class will be ignored
     * @returns {string} the URL
     */
				value: function reportableIntervalUrl(locationId, sourceId) {
					var url = this.baseUrl() + '/location/datum/interval?locationId=' + (locationId || this.locationId);
					var source = sourceId || this.sourceId;
					if (sourceId !== null && source) {
						url += '&sourceId=' + encodeURIComponent(source);
					}
					return url;
				}

				/**
     * Generate a URL for finding the available source IDs for a location or metadata filter.
     * 
     * @param {number} [locationId] a specific location ID to use; if not provided the `locationId`
        *                              property of this class will be used
     * @param {Date} [startDate] a start date to limit the search to
        * @param {Date} [endDate] an end date to limit the search to
     * @returns {string} the URL
     */

			}, {
				key: 'availableSourcesUrl',
				value: function availableSourcesUrl(locationId, startDate, endDate) {
					var result = this.baseUrl() + '/location/datum/sources?locationId=' + (locationId || this.locationId);
					if (startDate instanceof Date) {
						result += '&start=' + encodeURIComponent(dateTimeUrlFormat(startDate));
					}
					if (endDate instanceof Date) {
						result += '&end=' + encodeURIComponent(dateTimeUrlFormat(endDate));
					}
					return result;
				}

				/**
     * Generate a URL for querying for location datum, in either raw or aggregate form.
     * 
     * If the `datumFilter` has an `aggregate` value set, then aggregate results will be
     * returned by SolarNet.
     * 
     * @param {module:domain~DatumFilter} datumFilter the search criteria
     * @param {module:domain~SortDescriptor[]} [sorts] optional sort settings to use
     * @param {module:domain~Pagination} [pagination] optional pagination settings to use
     * @returns {string} the URL
     */

			}, {
				key: 'listDatumUrl',
				value: function listDatumUrl(datumFilter, sorts, pagination) {
					var result = this.baseUrl() + '/location/datum/list';
					var params = datumFilter ? datumFilter.toUriEncodingWithSorting(sorts, pagination) : '';
					if (params.length > 0) {
						result += '?' + params;
					}
					return result;
				}

				/**
     * Generate a URL for querying for the most recent datum.
     * 
     * @param {module:domain~DatumFilter} datumFilter the search criteria
     * @param {module:domain~SortDescriptor[]} [sorts] optional sort settings to use
     * @param {module:domain~Pagination} [pagination] optional pagination settings to use
     * @returns {string} the URL
     */

			}, {
				key: 'mostRecentDatumUrl',
				value: function mostRecentDatumUrl(datumFilter, sorts, pagination) {
					var result = this.baseUrl() + '/location/datum/mostRecent';
					var params = datumFilter ? datumFilter.toUriEncodingWithSorting(sorts, pagination) : '';
					if (params.length > 0) {
						result += '?' + params;
					}
					return result;
				}
			}]);
			return _class;
		}(superclass)
	);
};

/**
 * A concrete {@link module:net~UrlHelper} with the {@link module:net~LocationDatumUrlHelperMixin}, 
 * {@link module:net~QueryUrlHelperMixin}, and {@link module:net~LocationUrlHelperMixin} mixins.
 * 
 * @mixes module:net~LocationDatumUrlHelperMixin
 * @mixes module:net~QueryUrlHelperMixin
 * @mixes module:net~LocationUrlHelperMixin
 * @extends module:net~UrlHelper
 * @alias module:net~LocationDatumUrlHelper
 */

var LocationDatumUrlHelper = function (_LocationDatumUrlHelp) {
	inherits(LocationDatumUrlHelper, _LocationDatumUrlHelp);

	function LocationDatumUrlHelper() {
		classCallCheck(this, LocationDatumUrlHelper);
		return possibleConstructorReturn(this, (LocationDatumUrlHelper.__proto__ || Object.getPrototypeOf(LocationDatumUrlHelper)).apply(this, arguments));
	}

	return LocationDatumUrlHelper;
}(LocationDatumUrlHelperMixin(QueryUrlHelperMixin(LocationUrlHelperMixin(UrlHelper))));

var NodeIdsKey$1 = 'nodeIds';
var SourceIdsKey$2 = 'sourceIds';

/**
 * Create a NodeUrlHelperMixin class.
 *
 * @param {module:net~UrlHelper} superclass the UrlHelper class to mix onto
 * @return {module:net~NodeUrlHelperMixin} the mixin class
 */
var NodeUrlHelperMixin = function NodeUrlHelperMixin(superclass) {
    return (

        /**
         * A mixin class that adds support for SolarNode properties to a {@link module:net~UrlHelper}.
         * 
         * @mixin
         * @alias module:net~NodeUrlHelperMixin
         */
        function (_superclass) {
            inherits(_class, _superclass);

            function _class() {
                classCallCheck(this, _class);
                return possibleConstructorReturn(this, (_class.__proto__ || Object.getPrototypeOf(_class)).apply(this, arguments));
            }

            createClass(_class, [{
                key: 'nodeId',


                /**
                 * The first available node ID from the `nodeIds` property.
                 * Setting this replaces any existing node IDs with an array of just that value.
                 * @type {number}
                 */
                get: function get$$1() {
                    var nodeIds = this.nodeIds;
                    return Array.isArray(nodeIds) && nodeIds.length > 0 ? nodeIds[0] : null;
                },
                set: function set$$1(nodeId) {
                    this.parameter(NodeIdsKey$1, nodeId ? [nodeId] : null);
                }

                /**
                 * An array of node IDs, set on the `nodeIds` parameter
                 * @type {number[]}
                 */

            }, {
                key: 'nodeIds',
                get: function get$$1() {
                    return this.parameter(NodeIdsKey$1);
                },
                set: function set$$1(nodeIds) {
                    this.parameter(NodeIdsKey$1, nodeIds);
                }

                /**
                 * The first available source ID from the `sourceIds` property.
                 * Setting this replaces any existing node IDs with an array of just that value.
                 * @type {string}
                 */

            }, {
                key: 'sourceId',
                get: function get$$1() {
                    var sourceIds = this.sourceIds;
                    return Array.isArray(sourceIds) && sourceIds.length > 0 ? sourceIds[0] : null;
                },
                set: function set$$1(sourceId) {
                    this.parameter(SourceIdsKey$2, sourceId ? [sourceId] : sourceId);
                }

                /**
                 * An array of source IDs, set on the `sourceIds` parameter
                 * @type {string[]}
                 */

            }, {
                key: 'sourceIds',
                get: function get$$1() {
                    return this.parameter(SourceIdsKey$2);
                },
                set: function set$$1(sourceIds) {
                    this.parameter(SourceIdsKey$2, sourceIds);
                }
            }]);
            return _class;
        }(superclass)
    );
};

/**
 * Create a NodeDatumUrlHelperMixin class.
 *
 * @param {module:net~UrlHelper} superclass the UrlHelper class to mix onto
 * @return {module:net~NodeDatumMetadataUrlHelperMixin} the mixin class
 */
var NodeDatumMetadataUrlHelperMixin = function NodeDatumMetadataUrlHelperMixin(superclass) {
    return (

        /**
         * A mixin class that adds SolarNode datum metadata support to {@link module:net~UrlHelper}.
         * 
         * <p>Datum metadata is metadata associated with a specific node and source, i.e. 
         * a <code>nodeId</code> and a <code>sourceId</code>.
         * 
         * @mixin
         * @alias module:net~NodeDatumMetadataUrlHelperMixin
         */
        function (_superclass) {
            inherits(_class, _superclass);

            function _class() {
                classCallCheck(this, _class);
                return possibleConstructorReturn(this, (_class.__proto__ || Object.getPrototypeOf(_class)).apply(this, arguments));
            }

            createClass(_class, [{
                key: 'baseNodeDatumMetadataUrl',


                /**
                 * Get a base URL for datum metadata operations using a specific node ID.
                 * 
                 * @param {number} [nodeId] a specific node ID to use; if not provided the <code>nodeId</code> property of this class will be used
                 * @returns {string} the base URL
                 * @private
                 */
                value: function baseNodeDatumMetadataUrl(nodeId) {
                    return this.baseUrl() + '/datum/meta/' + (nodeId || this.nodeId);
                }
            }, {
                key: 'nodeDatumMetadataUrlWithSource',
                value: function nodeDatumMetadataUrlWithSource(nodeId, sourceId) {
                    var result = this.baseNodeDatumMetadataUrl(nodeId);
                    var source = sourceId || this.sourceId;
                    if (sourceId !== null && source) {
                        result += '?sourceId=' + encodeURIComponent(source);
                    }
                    return result;
                }

                /**
                 * Generate a URL for viewing datum metadata.
                    * 
                    * If no <code>sourceId</code> is provided, then the API will return all available datum metadata for all sources.
                 *
                 * @param {number} [nodeId] a specific node ID to use; if not provided the <code>nodeId</code> property of this class will be used
                 * @param {string} [sourceId] a specific source ID to use; 
                    *                            if not provided the <code>sourceId</code> property of this class will be used;
                    *                            if <code>null</code> then ignore any <code>sourceId</code> property of this class
                    * @returns {string} the URL
                 */

            }, {
                key: 'viewNodeDatumMetadataUrl',
                value: function viewNodeDatumMetadataUrl(nodeId, sourceId) {
                    return this.nodeDatumMetadataUrlWithSource(nodeId, sourceId);
                }

                /**
                 * Generate a URL for adding (merging) datum metadata via a <code>POST</code> request.
                    * 
                 * @param {number} [nodeId] a specific node ID to use; if not provided the <code>nodeId</code> property of this class will be used
                 * @param {string} [sourceId] a specific source ID to use; if not provided the <code>sourceId</code> property of this class will be used
                    * @returns {string} the URL
                 */

            }, {
                key: 'addNodeDatumMetadataUrl',
                value: function addNodeDatumMetadataUrl(nodeId, sourceId) {
                    return this.nodeDatumMetadataUrlWithSource(nodeId, sourceId);
                }

                /**
                 * Generate a URL for setting datum metadata via a <code>PUT</code> request.
                    * 
                 * @param {number} [nodeId] a specific node ID to use; if not provided the <code>nodeId</code> property of this class will be used
                 * @param {string} [sourceId] a specific source ID to use; if not provided the <code>sourceId</code> property of this class will be used
                    * @returns {string} the URL
                 */

            }, {
                key: 'replaceNodeDatumMetadataUrl',
                value: function replaceNodeDatumMetadataUrl(nodeId, sourceId) {
                    return this.nodeDatumMetadataUrlWithSource(nodeId, sourceId);
                }

                /**
                 * Generate a URL for deleting datum metadata via a <code>DELETE</code> request.
                    * 
                 * @param {number} [nodeId] a specific node ID to use; if not provided the <code>nodeId</code> property of this class will be used
                 * @param {string} [sourceId] a specific source ID to use; if not provided the <code>sourceId</code> property of this class will be used
                    * @returns {string} the URL
                 */

            }, {
                key: 'deleteNodeDatumMetadataUrl',
                value: function deleteNodeDatumMetadataUrl(nodeId, sourceId) {
                    return this.nodeDatumMetadataUrlWithSource(nodeId, sourceId);
                }

                /**
                 * Generate a URL for searching for datum metadata.
                 * 
                 * @param {number} [nodeId] a specific node ID to use; if not provided the <code>nodeId</code> property of this class will be used
                 * @param {string} [sourceId] a specific source ID to use; 
                    *                            if not provided the <code>sourceId</code> property of this class will be used;
                    *                            if <code>null</code> then ignore any <code>sourceId</code> property of this class
                 * @param {SortDescriptor[]} [sorts] optional sort settings to use
                 * @param {module:domain~Pagination} [pagination] optional pagination settings to use
                 * @returns {string} the URL
                 */

            }, {
                key: 'findNodeDatumMetadataUrl',
                value: function findNodeDatumMetadataUrl(nodeId, sourceId, sorts, pagination) {
                    var result = this.baseNodeDatumMetadataUrl(nodeId);
                    var params = '';
                    var source = sourceId || this.sourceId;
                    if (sourceId !== null && source) {
                        params += 'sourceId=' + encodeURIComponent(source);
                    }
                    if (Array.isArray(sorts)) {
                        sorts.forEach(function (sort, i) {
                            if (sort instanceof SortDescriptor) {
                                if (params.length > 0) {
                                    params += '&';
                                }
                                params += sort.toUriEncoding(i);
                            }
                        });
                    }
                    if (pagination instanceof Pagination) {
                        if (params.length > 0) {
                            params += '&';
                        }
                        params += pagination.toUriEncoding();
                    }
                    if (params.length > 0) {
                        result += '?' + params;
                    }
                    return result;
                }
            }]);
            return _class;
        }(superclass)
    );
};

/**
 * A concrete {@link module:net~UrlHelper} with the {@link module:net~NodeDatumMetadataUrlHelperMixin},  
 * {@link module:net~QueryUrlHelperMixin}, and {@link module:net~NodeUrlHelperMixin} mixins.
 * 
 * @mixes module:net~NodeDatumMetadataUrlHelperMixin
 * @mixes module:net~QueryUrlHelperMixin
 * @mixes module:net~NodeUrlHelperMixin
 * @extends module:net~UrlHelper
 * @alias module:net~NodeDatumMetadataUrlHelper
 */

var NodeDatumMetadataUrlHelper = function (_NodeDatumMetadataUrl) {
    inherits(NodeDatumMetadataUrlHelper, _NodeDatumMetadataUrl);

    function NodeDatumMetadataUrlHelper() {
        classCallCheck(this, NodeDatumMetadataUrlHelper);
        return possibleConstructorReturn(this, (NodeDatumMetadataUrlHelper.__proto__ || Object.getPrototypeOf(NodeDatumMetadataUrlHelper)).apply(this, arguments));
    }

    return NodeDatumMetadataUrlHelper;
}(NodeDatumMetadataUrlHelperMixin(QueryUrlHelperMixin(NodeUrlHelperMixin(UrlHelper))));

/**
 * Create a NodeDatumUrlHelperMixin class.
 *
 * @param {module:net~UrlHelper} superclass the UrlHelper class to mix onto
 * @return {module:net~NodeDatumUrlHelperMixin} the mixin class
 */
var NodeDatumUrlHelperMixin = function NodeDatumUrlHelperMixin(superclass) {
	return (

		/**
   * A mixin class that adds SolarNode datum query support to {@link module:net~UrlHelper}.
   * 
   * @mixin
   * @alias module:net~NodeDatumUrlHelperMixin
   */
		function (_superclass) {
			inherits(_class, _superclass);

			function _class() {
				classCallCheck(this, _class);
				return possibleConstructorReturn(this, (_class.__proto__ || Object.getPrototypeOf(_class)).apply(this, arguments));
			}

			createClass(_class, [{
				key: 'reportableIntervalUrl',


				/**
     * Generate a URL for the "reportable interval" for a node, optionally limited to a specific set of source IDs.
     *
        * If no source IDs are provided, then the reportable interval query will return an interval for
        * all available sources.
        *
     * @param {number} [nodeId] a specific node ID to use; if not provided the `nodeId` property of this class will be used
     * @param {string[]} [sourceIds] an array of source IDs to limit query to; if not provided the `sourceIds` property of this class will be used
     * @returns {string} the URL
     */
				value: function reportableIntervalUrl(nodeId, sourceIds) {
					var url = this.baseUrl() + '/range/interval?nodeId=' + (nodeId || this.nodeId);
					var sources = sourceIds || this.sourceIds;
					if (Array.isArray(sources) && sources.length > 0) {
						url += '&sourceIds=' + sources.map(function (e) {
							return encodeURIComponent(e);
						}).join(',');
					}
					return url;
				}

				/**
     * Generate a URL for finding the available source IDs for a node or metadata filter.
     * 
     * @param {module:domain~DatumFilter} datumFilter the search criteria, which can define `nodeId`, `startDate`, `endDate`,
     *                                                and `metadataFilter` properties to limit the results to; if `nodeId` not
     *                                                provided the `nodeIds` property of this class will be used
     * @param {string} [metadataFilter] the LDAP-style metadata filter
     * @returns {string} the URL
     */

			}, {
				key: 'availableSourcesUrl',
				value: function availableSourcesUrl(datumFilter) {
					var filter = datumFilter || this.datumFilter();
					var result = this.baseUrl() + '/range/sources';
					var params = filter.toUriEncoding();
					if (params.length > 0) {
						result += '?' + params;
					}
					return result;
				}

				/**
     * Generate a URL for querying for datum, in either raw or aggregate form.
     * 
     * If the `datumFilter` has an `aggregate` value set, then aggregate results will be
     * returned by SolarNet.
     * 
     * @param {module:domain~DatumFilter} datumFilter the search criteria
     * @param {module:domain~SortDescriptor[]} [sorts] optional sort settings to use
     * @param {module:domain~Pagination} [pagination] optional pagination settings to use
     * @returns {string} the URL
     */

			}, {
				key: 'listDatumUrl',
				value: function listDatumUrl(datumFilter, sorts, pagination) {
					var result = this.baseUrl() + '/datum/list';
					var filter = datumFilter || this.datumFilter();
					var params = filter.toUriEncodingWithSorting(sorts, pagination);
					if (params.length > 0) {
						result += '?' + params;
					}
					return result;
				}

				/**
     * Get a new {@link module:domain~DatumFilter} configured with properties of this instance.
     * 
     * This will configure the following properties:
     * 
     *  * `nodeIds`
     *  * `sourceIds`
     * 
     * @returns {module:domain~DatumFilter} the filter
     */

			}, {
				key: 'datumFilter',
				value: function datumFilter() {
					var filter = new DatumFilter();
					var v = void 0;

					v = this.nodeIds;
					if (v) {
						filter.nodeIds = v;
					}

					v = this.sourceIds;
					if (v) {
						filter.sourceIds = v;
					}

					return filter;
				}

				/**
     * Generate a URL for querying for the most recent datum.
     * 
     * @param {module:domain~DatumFilter} datumFilter the search criteria
     * @param {module:domain~SortDescriptor[]} [sorts] optional sort settings to use
     * @param {module:domain~Pagination} [pagination] optional pagination settings to use
     * @returns {string} the URL
     */

			}, {
				key: 'mostRecentDatumUrl',
				value: function mostRecentDatumUrl(datumFilter, sorts, pagination) {
					var result = this.baseUrl() + '/datum/mostRecent';
					var filter = datumFilter || this.datumFilter();
					var params = filter.toUriEncodingWithSorting(sorts, pagination);
					if (params.length > 0) {
						result += '?' + params;
					}
					return result;
				}
			}]);
			return _class;
		}(superclass)
	);
};

/**
 * A concrete {@link module:net~UrlHelper} with the {@link module:net~NodeDatumUrlHelperMixin}, 
 * {@link module:net~QueryUrlHelperMixin}, and {@link module:net~NodeUrlHelperMixin} mixins.
 * 
 * @mixes module:net~NodeDatumUrlHelperMixin
 * @mixes module:net~QueryUrlHelperMixin
 * @mixes module:net~NodeUrlHelperMixin
 * @extends module:net~UrlHelper
 * @alias module:net~NodeDatumUrlHelper
 */

var NodeDatumUrlHelper = function (_NodeDatumUrlHelperMi) {
	inherits(NodeDatumUrlHelper, _NodeDatumUrlHelperMi);

	function NodeDatumUrlHelper() {
		classCallCheck(this, NodeDatumUrlHelper);
		return possibleConstructorReturn(this, (NodeDatumUrlHelper.__proto__ || Object.getPrototypeOf(NodeDatumUrlHelper)).apply(this, arguments));
	}

	return NodeDatumUrlHelper;
}(NodeDatumUrlHelperMixin(QueryUrlHelperMixin(NodeUrlHelperMixin(UrlHelper))));

/** 
 * The SolarUser default path.
 * @type {string}
 * @alias module:net~SolarUserDefaultPath
 */
var SolarUserDefaultPath = '/solaruser';

/**
 * The {@link module:net~UrlHelper} parameters key for the SolarUser path.
 * @type {string}
 * @alias module:net~SolarUserPathKey
 */
var SolarUserPathKey = 'solarUserPath';

/** 
 * The SolarUser REST API path.
 * @type {string}
 * @alias module:net~SolarUserApiPathV1
 */
var SolarUserApiPathV1 = '/api/v1/sec';

var UserIdsKey$1 = 'userIds';

/**
 * Create a UserUrlHelperMixin class.
 *
 * @param {module:net~UrlHelper} superclass the UrlHelper class to mix onto
 * @return {module:net~UserUrlHelperMixin} the mixin class
 */
var UserUrlHelperMixin = function UserUrlHelperMixin(superclass) {
	return (

		/**
   * A mixin class that adds SolarUser specific support to {@link module:net~UrlHelper}.
   * 
   * @mixin
   * @alias module:net~UserUrlHelperMixin
   */
		function (_superclass) {
			inherits(_class, _superclass);

			function _class() {
				classCallCheck(this, _class);
				return possibleConstructorReturn(this, (_class.__proto__ || Object.getPrototypeOf(_class)).apply(this, arguments));
			}

			createClass(_class, [{
				key: 'baseUrl',


				/**
     * Get the base URL to the SolarUser v1 REST API.
     * 
     * The returned URL uses the configured environment to resolve
     * the `hostUrl` and a `solarUserPath` context path.
     * If the context path is not available, it will default to 
     * `/solaruser`.
     * 
     * @returns {string} the base URL to SolarUser
     */
				value: function baseUrl() {
					var path = this.env(SolarUserPathKey) || SolarUserDefaultPath;
					return get(_class.prototype.__proto__ || Object.getPrototypeOf(_class.prototype), 'baseUrl', this).call(this) + path + SolarUserApiPathV1;
				}

				/**
     * Generate a URL to get a list of all active nodes for the user account.
     *
     * @return {string} the URL to access the user's active nodes
     */

			}, {
				key: 'viewNodesUrl',
				value: function viewNodesUrl() {
					return this.baseUrl() + '/nodes';
				}

				/**
     * Generate a URL to get a list of all pending nodes for the user account.
     *
     * @return {string} the URL to access the user's pending nodes
     */

			}, {
				key: 'viewPendingNodesUrl',
				value: function viewPendingNodesUrl() {
					return this.baseUrl() + '/nodes/pending';
				}

				/**
     * Generate a URL to get a list of all archived nodes for the user account.
     *
     * @return {string} the URL to access the user's archived nodes
     */

			}, {
				key: 'viewArchivedNodesUrl',
				value: function viewArchivedNodesUrl() {
					return this.baseUrl() + '/nodes/archived';
				}

				/**
     * Generate a URL to update the archived status of a set of nodes via a `POST` request.
     *
     * @param {number|number[]|null} nodeId a specific node ID, or array of node IDs, to update; if not provided the 
     *                                      `nodeIds` property of this class will be used
     * @param {boolean} archived `true` to mark the nodes as archived; `false` to un-mark
     *                           and return to normal status
     * @return {string} the URL to update the nodes archived status
     */

			}, {
				key: 'updateNodeArchivedStatusUrl',
				value: function updateNodeArchivedStatusUrl(nodeId, archived) {
					var nodeIds = Array.isArray(nodeId) ? nodeId : nodeId ? [nodeId] : this.nodeIds;
					var result = this.baseUrl() + '/nodes/archived?nodeIds=' + nodeIds.join(',') + '&archived=' + (archived ? 'true' : 'false');
					return result;
				}
			}, {
				key: 'userId',


				/**
     * Get the default user ID.
     * 
     * This gets the first available user ID from the `userIds` property.
     * 
     * @returns {number} the default user ID, or `null`
     */
				get: function get$$1() {
					var userIds = this.parameter(UserIdsKey$1);
					return Array.isArray(userIds) && userIds.length > 0 ? userIds[0] : null;
				}

				/**
     * Set the user ID.
     * 
     * This will set the `userIds` property to a new array of just the given value.
     * 
     * @param {number} userId the user ID to set
     */
				,
				set: function set$$1(userId) {
					this.parameter(UserIdsKey$1, [userId]);
				}
			}, {
				key: 'userIds',
				get: function get$$1() {
					return this.parameter(UserIdsKey$1);
				},
				set: function set$$1(userIds) {
					this.parameter(UserIdsKey$1, userIds);
				}
			}]);
			return _class;
		}(superclass)
	);
};

/**
 * Create a NodeInstructionUrlHelperMixin class.
 *
 * @param {module:net~UrlHelper} superclass the UrlHelper class to mix onto
 * @return {module:net~NodeInstructionUrlHelperMixin} the mixin class
 */
var NodeInstructionUrlHelperMixin = function NodeInstructionUrlHelperMixin(superclass) {
	return (

		/**
   * A mixin class that adds SolarNode instruction support to {@link module:net~UrlHelper}.
   * 
   * @mixin
   * @alias module:net~NodeInstructionUrlHelperMixin
   */
		function (_superclass) {
			inherits(_class, _superclass);

			function _class() {
				classCallCheck(this, _class);
				return possibleConstructorReturn(this, (_class.__proto__ || Object.getPrototypeOf(_class)).apply(this, arguments));
			}

			createClass(_class, [{
				key: 'viewInstructionUrl',


				/**
     * Generate a URL to get all details for a specific instruction.
     * 
     * @param {number} instructionId the instruction ID to get
     * @returns {string} the URL
     */
				value: function viewInstructionUrl(instructionId) {
					return this.baseUrl() + '/instr/view?id=' + encodeURIComponent(instructionId);
				}

				/**
     * Generate a URL for viewing active instructions.
     * 
     * @param {number} [nodeId] a specific node ID to use; if not provided the <code>nodeId</code> property of this class will be used
     * @returns {string} the URL
     */

			}, {
				key: 'viewActiveInstructionsUrl',
				value: function viewActiveInstructionsUrl(nodeId) {
					return this.baseUrl() + '/instr/viewActive?nodeId=' + (nodeId || this.nodeId);
				}

				/**
     * Generate a URL for viewing pending instructions.
     * 
     * @param {number} [nodeId] a specific node ID to use; if not provided the <code>nodeId</code> property of this class will be used
     * @returns {string} the URL
     */

			}, {
				key: 'viewPendingInstructionsUrl',
				value: function viewPendingInstructionsUrl(nodeId) {
					return this.baseUrl() + '/instr/viewPending?nodeId=' + (nodeId || this.nodeId);
				}

				/**
     * Generate a URL for changing the state of an instruction.
     * 
     * @param {number} instructionId the instruction ID to update
     * @param {InstructionState} state the instruction state to set
     * @returns {string} the URL
     * @see the {@link InstructionStates} enum for possible state values
     */

			}, {
				key: 'updateInstructionStateUrl',
				value: function updateInstructionStateUrl(instructionId, state) {
					return this.baseUrl() + '/instr/updateState?id=' + encodeURIComponent(instructionId) + '&state=' + encodeURIComponent(state.name);
				}

				/**
     * Generate a URL for posting an instruction request.
     *
     * @param {string} topic the instruction topic.
     * @param {Object[]} [parameters] an array of parameter objects in the form <code>{name:n1, value:v1}</code>.
     * @param {number} [nodeId] a specific node ID to use; if not provided the <code>nodeId</code> property of this class will be used
     * @returns {string} the URL
     */

			}, {
				key: 'queueInstructionUrl',
				value: function queueInstructionUrl(topic, parameters, nodeId) {
					var url = this.baseUrl() + '/instr/add?nodeId=' + (nodeId || this.nodeId) + '&topic=' + encodeURIComponent(topic);
					var i, len;
					if (Array.isArray(parameters)) {
						for (i = 0, len = parameters.length; i < len; i++) {
							url += '&' + encodeURIComponent('parameters[' + i + '].name') + '=' + encodeURIComponent(parameters[i].name) + '&' + encodeURIComponent('parameters[' + i + '].value') + '=' + encodeURIComponent(parameters[i].value);
						}
					}
					return url;
				}

				/**
     * Create an instruction parameter suitable to passing to {@link NodeInstructionUrlHelperMixin#queueInstructionUrl}.
     * 
     * @param {string} name the parameter name 
     * @param {*} value the parameter value
     * @returns {object} with <code>name</code> and <code>value</code> properties
     */

			}], [{
				key: 'instructionParameter',
				value: function instructionParameter(name, value) {
					return { name: name, value: value };
				}
			}]);
			return _class;
		}(superclass)
	);
};

/**
 * A concrete {@link UrlHelper} with the {@link module:net~NodeInstructionUrlHelperMixin},  
 * {@link module:net~UserUrlHelperMixin}, and {@link module:net~NodeUrlHelperMixin} mixins.
 * 
 * @mixes module:net~NodeInstructionUrlHelperMixin
 * @mixes module:net~UserUrlHelperMixin
 * @mixes module:net~NodeUrlHelperMixin
 * @extends module:net~UrlHelper
 * @alias module:net~NodeInstructionUrlHelper
 */

var NodeInstructionUrlHelper = function (_NodeInstructionUrlHe) {
	inherits(NodeInstructionUrlHelper, _NodeInstructionUrlHe);

	function NodeInstructionUrlHelper() {
		classCallCheck(this, NodeInstructionUrlHelper);
		return possibleConstructorReturn(this, (NodeInstructionUrlHelper.__proto__ || Object.getPrototypeOf(NodeInstructionUrlHelper)).apply(this, arguments));
	}

	return NodeInstructionUrlHelper;
}(NodeInstructionUrlHelperMixin(UserUrlHelperMixin(NodeUrlHelperMixin(UrlHelper))));

/**
 * The static {@link NodeInstructionUrlHelperMixin#instructionParameter} method so it can be imported directly.
 * 
 * @alias module:net~instructionParameter
 */


var instructionParameter = NodeInstructionUrlHelper.instructionParameter;

/**
 * Create a NodeMetadataUrlHelperMixin class.
 *
 * @param {module:net~UrlHelper} superclass the UrlHelper class to mix onto
 * @return {module:net~NodeMetadataUrlHelperMixin} the mixin class
 */
var NodeMetadataUrlHelperMixin = function NodeMetadataUrlHelperMixin(superclass) {
	return (

		/**
   * A mixin class that adds SolarNode metadata support to {@link module:net~UrlHelper}.
   * 
   * @mixin
   * @alias module:net~NodeMetadataUrlHelperMixin
   */
		function (_superclass) {
			inherits(_class, _superclass);

			function _class() {
				classCallCheck(this, _class);
				return possibleConstructorReturn(this, (_class.__proto__ || Object.getPrototypeOf(_class)).apply(this, arguments));
			}

			createClass(_class, [{
				key: 'viewNodeMetadataUrl',


				/**
     * Generate a URL for viewing the configured node's metadata.
     *
     * @param {number} [nodeId] a specific node ID to use; if not provided the `nodeId` property of this class will be used
     * @returns {string} the URL
     */
				value: function viewNodeMetadataUrl(nodeId) {
					return this.baseUrl() + '/nodes/meta/' + (nodeId || this.nodeId);
				}

				/**
     * Generate a URL for adding metadata to a node via a `POST` request.
     *
     * @param {number} [nodeId] a specific node ID to use; if not provided the `nodeId` property of this class will be used
     * @returns {string} the URL
     */

			}, {
				key: 'addNodeMetadataUrl',
				value: function addNodeMetadataUrl(nodeId) {
					return this.viewNodeMetadataUrl(nodeId);
				}

				/**
     * Generate a URL for setting the metadata of a node via a `PUT` request.
     *
     * @param {number} [nodeId] a specific node ID to use; if not provided the `nodeId` property of this class will be used
     * @returns {string} the URL
     */

			}, {
				key: 'replaceNodeMetadataUrl',
				value: function replaceNodeMetadataUrl(nodeId) {
					return this.viewNodeMetadataUrl(nodeId);
				}

				/**
     * Generate a URL for deleting the metadata of a node via a `DELETE` request.
     *
     * @param {number} [nodeId] a specific node ID to use; if not provided the `nodeId` property of this class will be used
     * @returns {string} the URL
     */

			}, {
				key: 'deleteNodeMetadataUrl',
				value: function deleteNodeMetadataUrl(nodeId) {
					return this.viewNodeMetadataUrl(nodeId);
				}

				/**
     * Generate a URL for searching for node metadata.
     * 
     * @param {number|number[]} [nodeId] a specific node ID, or array of node IDs, to use; if not provided the 
     *                                   `nodeIds` property of this class will be used, unless `null`
     *                                   is passed in which case no node IDs will be added to the URL so that all available
     *                                   node metadata objects will be returned
     * @param {module:domain~SortDescriptor[]} [sorts] optional sort settings to use
     * @param {module:domain~Pagination} [pagination] optional pagination settings to use
     * @returns {string} the URL
     */

			}, {
				key: 'findNodeMetadataUrl',
				value: function findNodeMetadataUrl(nodeId, sorts, pagination) {
					var nodeIds = Array.isArray(nodeId) ? nodeId : nodeId ? [nodeId] : nodeId !== null ? this.nodeIds : undefined;
					var result = this.baseUrl() + '/nodes/meta';
					var params = '';
					if (Array.isArray(nodeIds)) {
						params += 'nodeIds=' + nodeIds.join(',');
					}
					if (Array.isArray(sorts)) {
						sorts.forEach(function (sort, i) {
							if (sort instanceof SortDescriptor) {
								if (params.length > 0) {
									params += '&';
								}
								params += sort.toUriEncoding(i);
							}
						});
					}
					if (pagination instanceof Pagination) {
						if (params.length > 0) {
							params += '&';
						}
						params += pagination.toUriEncoding();
					}
					if (params.length > 0) {
						result += '?' + params;
					}
					return result;
				}
			}]);
			return _class;
		}(superclass)
	);
};

/**
 * A concrete {@link module:net~UrlHelper} with the {@link module:net~NodeMetadataUrlHelperMixin},  
 * {@link module:net~UserUrlHelperMixin}, and {@link module:net~NodeUrlHelperMixin} mixins.
 * 
 * @mixes module:net~NodeMetadataUrlHelperMixin
 * @mixes module:net~UserUrlHelperMixin
 * @mixes module:net~NodeUrlHelperMixin
 * @extends module:net~UrlHelper
 * @alias module:net~NodeMetadataUrlHelper
 */

var NodeMetadataUrlHelper = function (_NodeMetadataUrlHelpe) {
	inherits(NodeMetadataUrlHelper, _NodeMetadataUrlHelpe);

	function NodeMetadataUrlHelper() {
		classCallCheck(this, NodeMetadataUrlHelper);
		return possibleConstructorReturn(this, (NodeMetadataUrlHelper.__proto__ || Object.getPrototypeOf(NodeMetadataUrlHelper)).apply(this, arguments));
	}

	return NodeMetadataUrlHelper;
}(NodeMetadataUrlHelperMixin(UserUrlHelperMixin(NodeUrlHelperMixin(UrlHelper))));

/**
 * Create a UserAuthTokenUrlHelperMixin class.
 *
 * @param {module:net~UrlHelper} superclass the UrlHelper class to mix onto
 * @return {module:net~UserAuthTokenUrlHelperMixin} the mixin class
 */
var UserAuthTokenUrlHelperMixin = function UserAuthTokenUrlHelperMixin(superclass) {
    return (

        /**
         * A mixin class that adds security token support to a SolarUser {@link module:net~UrlHelper}.
         * 
         * @mixin
         * @alias module:net~UserAuthTokenUrlHelperMixin
         */
        function (_superclass) {
            inherits(_class, _superclass);

            function _class() {
                classCallCheck(this, _class);
                return possibleConstructorReturn(this, (_class.__proto__ || Object.getPrototypeOf(_class)).apply(this, arguments));
            }

            createClass(_class, [{
                key: 'listAllAuthTokensUrl',


                /**
                 * Generate a URL for listing all available auth tokens.
                 * 
                * @returns {string} the URL
                 */
                value: function listAllAuthTokensUrl() {
                    return this.baseUrl() + '/user/auth-tokens';
                }

                /**
                 * Generate a URL for creating a new auth token, via a `POST` request.
                 * 
                 * The request body accepts a {@link module:domain~SecurityPolicy} JSON document.
                 * 
                 * @param {AuthTokenType} type the auth token type to generate
                * @returns {string} the URL
                 */

            }, {
                key: 'generateAuthTokenUrl',
                value: function generateAuthTokenUrl(type) {
                    return this.baseUrl() + '/user/auth-tokens/generate/' + type.name;
                }

                /**
                 * Generate a URL for accessing an auth token.
                 * 
                 * @param {string} tokenId the token ID
                * @returns {string} the URL
                 * @private
                 */

            }, {
                key: 'authTokenUrl',
                value: function authTokenUrl(tokenId) {
                    return this.baseUrl() + '/user/auth-tokens/' + encodeURIComponent(tokenId);
                }

                /**
                 * Generate a URL for deleting an auth token, via a `DELETE` request.
                 * 
                 * @param {string} tokenId the token ID to delete
                * @returns {string} the URL
                 */

            }, {
                key: 'deleteAuthTokenUrl',
                value: function deleteAuthTokenUrl(tokenId) {
                    return this.authTokenUrl(tokenId);
                }

                /**
                 * Generate a URL for updating (merging) a security policy on an auth token,
                 * via a `PATCH` request.
                 * 
                 * The request body accepts a {@link module:net~SecurityPolicy} JSON document.
                 * 
                 * @param {string} tokenId the ID of the token to update
                * @returns {string} the URL
                 */

            }, {
                key: 'updateAuthTokenSecurityPolicyUrl',
                value: function updateAuthTokenSecurityPolicyUrl(tokenId) {
                    return this.authTokenUrl(tokenId);
                }

                /**
                 * Generate a URL for replacing a security policy on an auth token,
                 * via a `PUT` request.
                 * 
                 * The request body accepts a {@link module:domain~SecurityPolicy} JSON document.
                 * 
                 * @param {string} tokenId the ID of the token to update
                * @returns {string} the URL
                 */

            }, {
                key: 'replaceAuthTokenSecurityPolicyUrl',
                value: function replaceAuthTokenSecurityPolicyUrl(tokenId) {
                    return this.authTokenUrl(tokenId);
                }

                /**
                 * Generate a URL for updating the status of an auth token,
                 * via a `POST` request.
                 * 
                 * @param {string} tokenId the ID of the token to update
                 * @param {AuthTokenStatus} status the status to change to
                * @returns {string} the URL
                 */

            }, {
                key: 'updateAuthTokenStatusUrl',
                value: function updateAuthTokenStatusUrl(tokenId, status) {
                    return this.authTokenUrl(tokenId) + '?status=' + encodeURIComponent(status.name);
                }
            }]);
            return _class;
        }(superclass)
    );
};

/**
 * A concrete {@link module:net~UrlHelper} with the {@link module:net~UserAuthTokenUrlHelperMixin} and  
 * {@link module:net~UserUrlHelperMixin} mixins.
 * 
 * @mixes module:net~UserAuthTokenUrlHelperMixin
 * @mixes module:net~UserUrlHelperMixin
 * @extends module:net~UrlHelper
 * @alias module:net~UserAuthTokenUrlHelper
 */

var UserAuthTokenUrlHelper = function (_UserAuthTokenUrlHelp) {
    inherits(UserAuthTokenUrlHelper, _UserAuthTokenUrlHelp);

    function UserAuthTokenUrlHelper() {
        classCallCheck(this, UserAuthTokenUrlHelper);
        return possibleConstructorReturn(this, (UserAuthTokenUrlHelper.__proto__ || Object.getPrototypeOf(UserAuthTokenUrlHelper)).apply(this, arguments));
    }

    return UserAuthTokenUrlHelper;
}(UserAuthTokenUrlHelperMixin(UserUrlHelperMixin(UrlHelper)));

/** @module net */

/* eslint no-console: 0 */

var logLevel = 2;

function consoleLog(level) {
    if (level > logLevel) {
        return;
    }
    if (!console) {
        return;
    }

    var logFn = void 0;
    switch (level) {
        case 1:
            logFn = console.error;
            break;
        case 2:
            logFn = console.warn;
            break;
        case 3:
            logFn = console.info;
            break;
    }
    if (!logFn) {
        logFn = console.log;
    }
    if (!logFn) {
        return; // no console available
    }

    for (var _len = arguments.length, args = Array(_len > 1 ? _len - 1 : 0), _key = 1; _key < _len; _key++) {
        args[_key - 1] = arguments[_key];
    }

    logFn.apply(undefined, args); // TODO formatting like sn.format.fmt.apply(this, arguments)?
}

var logLevels = Object.freeze({
    DEBUG: 4,
    INFO: 3,
    WARN: 2,
    ERROR: 1,
    OFF: 0
});

/**
 * An application logger.
 * 
 * Logging levels range from 0-4 and is controlled at the application level.
 * Level `0` is off, `1` is error, `2` is warn, `3` is info,  and `4` is debug.
 * The default level starts as `2`.
 */

var Logger = function () {
    function Logger() {
        classCallCheck(this, Logger);
    }

    createClass(Logger, null, [{
        key: 'debug',
        value: function debug() {
            for (var _len2 = arguments.length, args = Array(_len2), _key2 = 0; _key2 < _len2; _key2++) {
                args[_key2] = arguments[_key2];
            }

            consoleLog.apply(undefined, [4].concat(args));
        }
    }, {
        key: 'info',
        value: function info() {
            for (var _len3 = arguments.length, args = Array(_len3), _key3 = 0; _key3 < _len3; _key3++) {
                args[_key3] = arguments[_key3];
            }

            consoleLog.apply(undefined, [3].concat(args));
        }
    }, {
        key: 'warn',
        value: function warn() {
            for (var _len4 = arguments.length, args = Array(_len4), _key4 = 0; _key4 < _len4; _key4++) {
                args[_key4] = arguments[_key4];
            }

            consoleLog.apply(undefined, [2].concat(args));
        }
    }, {
        key: 'error',
        value: function error() {
            for (var _len5 = arguments.length, args = Array(_len5), _key5 = 0; _key5 < _len5; _key5++) {
                args[_key5] = arguments[_key5];
            }

            consoleLog.apply(undefined, [1].concat(args));
        }
    }]);
    return Logger;
}();

Object.defineProperties(Logger, {
    /**
     * The global logging level. Set to `0` to disable all logging.
     * 
     * @memberof module:util~Logger
     * @type {number}
     */
    level: {
        get: function get$$1() {
            return logLevel;
        },
        set: function set$$1(v) {
            logLevel = typeof v === 'number' ? v : 0;
        }
    }
});

/** @module util */

exports.Aggregations = Aggregations;
exports.Aggregation = Aggregation;
exports.AuthTokenStatuses = AuthTokenStatuses;
exports.AuthTokenStatus = AuthTokenStatus;
exports.AuthTokenTypes = AuthTokenTypes;
exports.AuthTokenType = AuthTokenType;
exports.DatumFilter = DatumFilter;
exports.GeneralMetadata = GeneralMetadata;
exports.stringMapToObject = stringMapToObject;
exports.objectToStringMap = objectToStringMap;
exports.InstructionStates = InstructionStates;
exports.InstructionState = InstructionState;
exports.Location = Location;
exports.LocationPrecisions = LocationPrecisions;
exports.LocationPrecision = LocationPrecision;
exports.Pagination = Pagination;
exports.SecurityPolicy = SecurityPolicy;
exports.SecurityPolicyBuilder = SecurityPolicyBuilder;
exports.SortDescriptor = SortDescriptor;
exports.timestampFormat = timestampFormat;
exports.dateTimeFormat = dateTimeFormat;
exports.dateTimeUrlFormat = dateTimeUrlFormat;
exports.dateFormat = dateFormat;
exports.timestampParse = timestampParse;
exports.dateTimeParse = dateTimeParse;
exports.dateParser = dateParser;
exports.iso8601Date = iso8601Date;
exports.dateTimeUrlParse = d3TimeFormat.isoParse;
exports.dateParse = d3TimeFormat.isoParse;
exports.displayScaleForValue = displayScaleForValue;
exports.displayUnitsForScale = displayUnitsForScale;
exports.AuthorizationV2Builder = AuthorizationV2Builder;
exports.Environment = Environment;
exports.HttpHeaders = HttpHeaders;
exports.HttpContentType = HttpContentType;
exports.HttpMethod = HttpMethod;
exports.LocationDatumMetadataUrlHelperMixin = LocationDatumMetadataUrlHelperMixin;
exports.LocationDatumMetadataUrlHelper = LocationDatumMetadataUrlHelper;
exports.LocationDatumUrlHelperMixin = LocationDatumUrlHelperMixin;
exports.LocationDatumUrlHelper = LocationDatumUrlHelper;
exports.LocationUrlHelperMixin = LocationUrlHelperMixin;
exports.NodeDatumMetadataUrlHelperMixin = NodeDatumMetadataUrlHelperMixin;
exports.NodeDatumMetadataUrlHelper = NodeDatumMetadataUrlHelper;
exports.NodeDatumUrlHelperMixin = NodeDatumUrlHelperMixin;
exports.NodeDatumUrlHelper = NodeDatumUrlHelper;
exports.NodeInstructionUrlHelperMixin = NodeInstructionUrlHelperMixin;
exports.NodeInstructionUrlHelper = NodeInstructionUrlHelper;
exports.instructionParameter = instructionParameter;
exports.NodeMetadataUrlHelperMixin = NodeMetadataUrlHelperMixin;
exports.NodeMetadataUrlHelper = NodeMetadataUrlHelper;
exports.NodeUrlHelperMixin = NodeUrlHelperMixin;
exports.QueryUrlHelperMixin = QueryUrlHelperMixin;
exports.SolarQueryDefaultPath = SolarQueryDefaultPath;
exports.SolarQueryPathKey = SolarQueryPathKey;
exports.SolarQueryApiPathV1 = SolarQueryApiPathV1;
exports.SolarQueryPublicPathKey = SolarQueryPublicPathKey;
exports.UserAuthTokenUrlHelperMixin = UserAuthTokenUrlHelperMixin;
exports.UserAuthTokenUrlHelper = UserAuthTokenUrlHelper;
exports.UserUrlHelperMixin = UserUrlHelperMixin;
exports.SolarUserDefaultPath = SolarUserDefaultPath;
exports.SolarUserPathKey = SolarUserPathKey;
exports.SolarUserApiPathV1 = SolarUserApiPathV1;
exports.UrlHelper = UrlHelper;
exports.urlQuery = urlQuery;
exports.ComparableEnum = ComparableEnum;
exports.Configuration = Configuration;
exports.Enum = Enum;
exports.Logger = Logger;
exports.logLevels = logLevels;
exports.MultiMap = MultiMap;
exports.PropMap = PropMap$2;

Object.defineProperty(exports, '__esModule', { value: true });

})));
