/**
 * Follow a 'trail' of properties starting at given object.
 * If one of the values on the trail is 'falsy' then
 * this value is returned instead of trying to keep following the
 * trail down.
 */
function deref(obj, props) {
	var it = obj;
	for (var i = 0; it && i < props.length; i++) {
		it = it[props[i]];
	}
	return it;
}

module.exports = deref;