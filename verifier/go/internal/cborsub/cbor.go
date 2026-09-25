// Package cborsub decodes the strict CBOR subset a DeviceResponse needs:
// maps, arrays, byte and text strings, unsigned integers, tags 0 and 24,
// booleans and null. Nothing else.
//
// The redirect path never decodes CBOR from the holder — the verifier only
// encodes (oid4vp/transcript.go). The ISO 18013-7 Annex C path does: the
// DeviceResponse arrives inside an HPKE envelope the wallet built, and every
// byte of it after decryption is attacker-supplied. fxamacker/cbor/v2 would be
// this module's first third-party dependency; the subset actually needed is
// small enough to decode here, under hard limits, failing closed on anything
// outside the subset. ADR-002 records the decision.
//
// Strictness rules, each a fail-closed rejection:
//
//   - definite lengths only (indefinite 0x1f arguments are rejected);
//   - shortest-form head only (0x18 0x41 is rejected; 0x18 0x18 also, a
//     non-minimal argument is never produced by a conformant encoder);
//   - no negative integers, floats, "simple" values other than false/true/null,
//     undefined, or tags other than 0 (over a tstr) and 24;
//   - tag 24 content must be a byte string whose bytes decode as this same
//     subset (RFC 8949 §8.1 requires tagged CBOR to be valid CBOR);
//   - duplicate map keys, of any kind, are rejected;
//   - every length, depth and size limit below is a hard error, not a truncation.
package cborsub

import (
	"errors"
	"fmt"
)

// Limits are the hard caps one decode runs under. Zero fields mean the
// defaults, so DefaultLimits is the ordinary way to get one.
type Limits struct {
	// MaxDepth bounds nesting of arrays, maps and tag 24.
	MaxDepth int
	// MaxItems bounds the element/pair count of one array or map.
	MaxItems int
	// MaxString bounds one byte or text string in bytes.
	MaxString int
}

// DefaultLimits are sized for a DeviceResponse: documents with COSE structures
// as byte strings, a handful of zkDocuments, nothing nested deeply. They are
// deliberately two orders of magnitude above anything a conformant response
// carries and far below anything that could exhaust a verification slot.
func DefaultLimits() Limits {
	return Limits{MaxDepth: 32, MaxItems: 1024, MaxString: 1 << 20}
}

func (l Limits) withDefaults() Limits {
	if l.MaxDepth == 0 {
		l.MaxDepth = 32
	}
	if l.MaxItems == 0 {
		l.MaxItems = 1024
	}
	if l.MaxString == 0 {
		l.MaxString = 1 << 20
	}
	return l
}

// Kind says which arm of Value is populated.
type Kind int

const (
	KUint Kind = iota
	KBool
	KNull
	KText
	KBytes
	KArray
	KMap
	KTag24
	KTag0
)

func (k Kind) String() string {
	switch k {
	case KUint:
		return "uint"
	case KBool:
		return "bool"
	case KNull:
		return "null"
	case KText:
		return "text"
	case KBytes:
		return "bytes"
	case KArray:
		return "array"
	case KMap:
		return "map"
	case KTag24:
		return "tag24"
	case KTag0:
		return "tag0"
	}
	return "unknown"
}

// KV is one map pair. Keys of every kind are kept; the strict map lookups this
// package adds are text-keyed.
type KV struct {
	Key   Value
	Value Value
}

// Value is one decoded CBOR item. Exactly the field named by Kind is
// populated; everything else is the zero value. Raw holds the item's exact
// encoded bytes, so a caller can carry a decoded substructure opaquely without
// re-encoding it into a different shape.
type Value struct {
	Kind  Kind
	Uint  uint64
	Bool  bool
	Text  string
	Bytes []byte
	Array []Value
	Map   []KV
	// Raw is the item's exact encoded bytes. It is populated by item() for
	// every value the decoder returns; reading it costs nothing because the
	// bytes are a slice of the input.
	Raw []byte
}

// ErrDecode is the base of every error this package returns. Callers
// distinguish mis encoded input from anything else with errors.Is; the
// specific message is for the server log, not for the holder.
var ErrDecode = errors.New("cborsub: not the accepted CBOR subset")

func fail(format string, args ...any) error {
	return fmt.Errorf("%w: %s", ErrDecode, fmt.Sprintf(format, args...))
}

// MapGet returns the value under the given text key: (value, true, nil) when
// present, (nil, false, nil) when the key is absent. A map with a non-text key
// of any kind returns an error, because a response carrying one is outside
// what this verifier parses.
func (v *Value) MapGet(key string) (*Value, bool, error) {
	if v.Kind != KMap {
		return nil, false, fail("MapGet on a %s", v.Kind)
	}
	// Every key is checked before any match is returned, so a non-text key
	// is refused wherever it sits relative to the one asked for.
	var found *Value
	for i := range v.Map {
		if v.Map[i].Key.Kind != KText {
			return nil, false, fail("map key of kind %s, only text keys are accepted", v.Map[i].Key.Kind)
		}
		if v.Map[i].Key.Text == key {
			found = &v.Map[i].Value
		}
	}
	return found, found != nil, nil
}

// Decode parses one CBOR item and requires the input to be exactly that item:
// trailing bytes are an error, so a response cannot smuggle a second root.
func Decode(data []byte, limits Limits) (*Value, error) {
	l := limits.withDefaults()
	d := decoder{data: data, limits: l}
	v, err := d.item(0)
	if err != nil {
		return nil, err
	}
	if d.pos != len(d.data) {
		return nil, fail("%d trailing bytes after the root item", len(d.data)-d.pos)
	}
	return v, nil
}

type decoder struct {
	data []byte
	pos  int
	// headLen is the byte length of the last head read; item() uses it to
	// find where the current item started once its body is consumed.
	headLen int
	limits  Limits
	// tagNesting counts tag-24 layers currently being validated across the
	// whole decode. The per-item depth counter resets inside every nested
	// Decode, so without this an 8 MB body of nested tag24s would buy each
	// level a fresh depth budget — an allocation and stack amplifier on the
	// unauthenticated pre-HPKE parse path.
	tagNesting int
}

func (d *decoder) byteAt(what string) (byte, error) {
	if d.pos >= len(d.data) {
		return 0, fail("input ends inside %s", what)
	}
	b := d.data[d.pos]
	d.pos++
	return b, nil
}

// head reads one definite, shortest-form head and returns (major, argument).
type head struct {
	major byte
	arg   uint64
}

func (d *decoder) head(what string) (head, error) {
	b, err := d.byteAt(what)
	if err != nil {
		return head{}, err
	}
	start := d.pos - 1
	h := head{major: b >> 5, arg: uint64(b & 0x1f)}
	switch info := b & 0x1f; {
	case info < 24:
		// shortest form for this value
	case info == 24:
		n, err := d.readUint(1, what)
		if err != nil {
			return head{}, err
		}
		if n < 24 {
			return head{}, fail("non-shortest-form uint argument %d in %s", n, what)
		}
		h.arg = n
	case info == 25:
		n, err := d.readUint(2, what)
		if err != nil {
			return head{}, err
		}
		if n < 1<<8 {
			return head{}, fail("non-shortest-form uint argument %d in %s", n, what)
		}
		h.arg = n
	case info == 26:
		n, err := d.readUint(4, what)
		if err != nil {
			return head{}, err
		}
		if n < 1<<16 {
			return head{}, fail("non-shortest-form uint argument %d in %s", n, what)
		}
		h.arg = n
	case info == 27:
		n, err := d.readUint(8, what)
		if err != nil {
			return head{}, err
		}
		if n < 1<<32 {
			return head{}, fail("non-shortest-form uint argument %d in %s", n, what)
		}
		h.arg = n
	default:
		return head{}, fail("indefinite length or reserved additional info 0x1f in %s", what)
	}
	d.headLen = d.pos - start
	return h, nil
}

func (d *decoder) readUint(n int, what string) (uint64, error) {
	if d.pos+n > len(d.data) {
		return 0, fail("input ends inside the %d-byte argument of %s", n, what)
	}
	var v uint64
	for i := 0; i < n; i++ {
		v = v<<8 | uint64(d.data[d.pos+i])
	}
	d.pos += n
	return v, nil
}

func (d *decoder) item(depth int) (*Value, error) {
	if depth > d.limits.MaxDepth {
		return nil, fail("nesting deeper than %d", d.limits.MaxDepth)
	}
	h, err := d.head("an item")
	if err != nil {
		return nil, err
	}
	start := d.pos - d.headLen
	v, err := d.itemBody(h, depth)
	if err != nil {
		return nil, err
	}
	// The item's exact encoded bytes, head included: a slice of the input,
	// not a copy.
	v.Raw = d.data[start:d.pos]
	return v, nil
}

// itemBody decodes the body of one item whose head is already consumed. The
// position bookkeeping (start above) lives in item so every arm returns
// through one place.
func (d *decoder) itemBody(h head, depth int) (*Value, error) {
	switch h.major {
	case 0:
		return &Value{Kind: KUint, Uint: h.arg}, nil
	case 1:
		return nil, fail("negative integer -%d-1, only unsigned integers are accepted", h.arg)
	case 2, 3:
		n, err := d.takeString(h, depth)
		if err != nil {
			return nil, err
		}
		raw := d.data[d.pos-n : d.pos]
		if h.major == 2 {
			b := make([]byte, n)
			copy(b, raw)
			return &Value{Kind: KBytes, Bytes: b}, nil
		}
		return &Value{Kind: KText, Text: string(raw)}, nil
	case 4:
		if h.arg > uint64(d.limits.MaxItems) {
			return nil, fail("array of %d items, more than the limit of %d", h.arg, d.limits.MaxItems)
		}
		arr := make([]Value, 0, h.arg)
		for i := uint64(0); i < h.arg; i++ {
			ev, err := d.item(depth + 1)
			if err != nil {
				return nil, err
			}
			arr = append(arr, *ev)
		}
		return &Value{Kind: KArray, Array: arr}, nil
	case 5:
		if h.arg > uint64(d.limits.MaxItems) {
			return nil, fail("map of %d pairs, more than the limit of %d", h.arg, d.limits.MaxItems)
		}
		m := make([]KV, 0, h.arg)
		for i := uint64(0); i < h.arg; i++ {
			k, err := d.item(depth + 1)
			if err != nil {
				return nil, err
			}
			v, err := d.item(depth + 1)
			if err != nil {
				return nil, err
			}
			m = append(m, KV{Key: *k, Value: *v})
		}
		// A duplicate key would make MapGet order-dependent, which is exactly
		// the kind of ambiguity a strict decoder exists to refuse. Keys of any
		// kind are compared by their encoded bytes, which is exact because
		// only shortest-form heads are accepted.
		seen := make(map[string]struct{}, len(m))
		for _, kv := range m {
			if _, dup := seen[string(kv.Key.Raw)]; dup {
				return nil, fail("duplicate map key %x", kv.Key.Raw)
			}
			seen[string(kv.Key.Raw)] = struct{}{}
		}
		return &Value{Kind: KMap, Map: m}, nil
	case 6:
		// Tag 0 is an RFC 8949 §3.4.1 standard date/time; multipaz emits
		// ZkDocumentData.timestamp as tag 0 over a tstr (ZkDocumentData.toDataItem
		// builds Tagged(0, Tstr)). The tstr carries "yyyy-MM-ddTHH:mm:ssZ" (the
		// format Longfellow proofs bind); it is decoded like a plain tstr, and
		// KTag0 marks the provenance so a caller can insist on it where the
		// shape requires a date.
		if h.arg == 0 {
			inner, err := d.item(depth + 1)
			if err != nil {
				return nil, err
			}
			if inner.Kind != KText {
				return nil, fail("tag 0 over a %s, the tagged item must be a text string", inner.Kind)
			}
			return &Value{Kind: KTag0, Text: inner.Text}, nil
		}
		if h.arg != 24 {
			return nil, fail("tag %d, only tags 0 and 24 are accepted", h.arg)
		}
		inner, err := d.item(depth + 1)
		if err != nil {
			return nil, err
		}
		if inner.Kind != KBytes {
			return nil, fail("tag 24 over a %s, the tagged item must be a byte string", inner.Kind)
		}
		// RFC 8949 §8.1: the bytes under tag 24 must themselves be valid CBOR.
		// The nested decode shares this decoder's tagNesting budget and
		// continues the array/map depth instead of getting fresh ones:
		// nesting must respect one global bound, not restart it at every
		// tag 24 boundary.
		d.tagNesting++
		if d.tagNesting > d.limits.MaxDepth {
			d.tagNesting--
			return nil, fail("tag 24 nesting deeper than %d", d.limits.MaxDepth)
		}
		sub := decoder{data: inner.Bytes, limits: d.limits, tagNesting: d.tagNesting}
		_, nestedErr := sub.item(depth + 1)
		d.tagNesting--
		if nestedErr != nil {
			return nil, fail("tag 24 content is not valid CBOR of this subset: %v", nestedErr)
		}
		if sub.pos != len(sub.data) {
			return nil, fail("tag 24 content has %d trailing bytes", len(sub.data)-sub.pos)
		}
		return &Value{Kind: KTag24, Bytes: inner.Bytes}, nil
	case 7:
		switch h.arg {
		case 20:
			return &Value{Kind: KBool, Bool: false}, nil
		case 21:
			return &Value{Kind: KBool, Bool: true}, nil
		case 22:
			return &Value{Kind: KNull}, nil
		default:
			return nil, fail("simple or float value with additional info %d, only false, true and null are accepted", h.arg)
		}
	}
	return nil, fail("unreachable CBOR major type %d", h.major)
}

// takeString validates and consumes a byte/text string of h.arg bytes.
func (d *decoder) takeString(h head, depth int) (int, error) {
	if h.arg > uint64(d.limits.MaxString) {
		return 0, fail("%s of %d bytes, more than the limit of %d", []string{"byte", "text"}[h.major-2], h.arg, d.limits.MaxString)
	}
	if h.arg > uint64(len(d.data)-d.pos) {
		return 0, fail("%s claims %d bytes but only %d remain", []string{"byte", "text"}[h.major-2], h.arg, len(d.data)-d.pos)
	}
	n := int(h.arg)
	d.pos += n
	return n, nil
}
