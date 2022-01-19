import math


def distance(a, b):
	return ((a[1] - b[1]) * (a[1] - b[1]) + (a[0] - b[0]) * (a[0] - b[0])) ** 0.5


def dot(va, vb):
	return va[0] * vb[0] + va[1] * vb[1]


def abs(va):
	return (va[0] * va[0] + va[1] * va[1]) ** 0.5


def norm(va):
	return va[0] / abs(va), va[1] / abs(va)


def cos(src, a, b):
	va = (a[0] - src[0], a[1] - src[1])
	vb = (b[0] - src[0], b[1] - src[1])
	r = dot(va, vb) / (abs(va) * abs(vb))
	if r < -1:
		r = -1
	elif r > 1:
		r = 1
	return r


def angle(src, a, b):
	return math.degrees(math.acos(cos(src, a, b)))


def translate(s, t):
	return s[0] + t[0], s[1] + t[1]


dirs = [
	(0, 1),
	(1, 1),
	(1, 0),
	(1, -1),
	(0, -1),
	(-1, -1),
	(-1, 0),
	(-1, 1),
]


def generate_heuristics(radius_squared):
	rsq = radius_squared
	r = int(rsq ** 0.5)
	d = r * 2 + 1

	# generate all possible locations
	all_loc = []
	for x in range(-r, r + 1):
		lim = int((rsq - x * x) ** 0.5)
		for y in range(-lim, lim + 1):
			if (x, y) != (0, 0):
				all_loc.append((x, y))

	l_dump = []
	d_dump = []
	dest_dump = []

	for dx, dy in dirs:
		translate_by = (r+1, r+1)
		dest = (dx * d * 2, dy * d * 2)
		src = (0, 0)
		my = sorted(all_loc, key=lambda l: distance(l, src) / (1 + math.fabs(cos(src, l, dest))), reverse=True)
		my = [
			translate(a, translate_by)
			for a in my
			if -45 <= angle(src, dest, a) <= 45
		]
		l_dump.append(my)

		my = sorted(all_loc, key=lambda l: distance(l, dest), reverse=True)
		my = [translate(a, translate_by) for a in my]
		dest_dump.append(my[-5:])

		vec = norm((dx, dy))
		my = sorted(dirs, key=lambda l: dot(vec, norm(l)), reverse=True)
		d_dump.append(my[-5:])

	file = open(f"generated/Heuristics{rsq}.java", 'w')
	file.write(f'''package gen5.common.bellmanford;


public class Heuristics{rsq} implements HeuristicsProvider {{
''')

	file.write("\n\tprivate final int[][] locationDumpX = {\n")
	for arr in l_dump:
		out = "\t\t{ "
		file.write("")
		for x, _ in arr:
			out += f"{x}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write("\n\tprivate final int[][] locationDumpY = {\n")
	for arr in l_dump:
		out = "\t\t{ "
		file.write("")
		for _, y in arr:
			out += f"{y}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write("\n\tprivate final int[][] directionDumpX = {\n")
	for arr in d_dump:
		out = "\t\t{ "
		file.write("")
		for x, _ in arr:
			out += f"{x}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write("\n\tprivate final int[][] directionDumpY = {\n")
	for arr in d_dump:
		out = "\t\t{ "
		file.write("")
		for _, y in arr:
			out += f"{y}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write("\n\tprivate final int[][] destinationDumpX = {\n")
	for arr in dest_dump:
		out = "\t\t{ "
		file.write("")
		for x, _ in arr:
			out += f"{x}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write("\n\tprivate final int[][] destinationDumpY = {\n")
	for arr in dest_dump:
		out = "\t\t{ "
		file.write("")
		for _, y in arr:
			out += f"{y}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write(
		'''

	@Override
	public int[] getDestinationsX(int d) {
		return destinationDumpX[d];
	}

	@Override
	public int[] getDestinationsY(int d) {
		return destinationDumpY[d];
	}

	@Override
	public int[] getLocationsX(int d) {
		return locationDumpX[d];
	}

	@Override
	public int[] getLocationsY(int d) {
		return locationDumpY[d];
	}

	@Override
	public int[] getDirectionsX(int d) {
		return directionDumpX[d];
	}

	@Override
	public int[] getDirectionsY(int d) {
		return directionDumpY[d];
	}

'''
	)

	file.write('}\n')
	file.close()


generate_heuristics(34)
