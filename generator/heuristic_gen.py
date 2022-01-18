

def distance(a, b):
	return ((a[1]-b[1])*(a[1]-b[1]) + (a[0]-b[0])*(a[0]-b[0]))**0.5


def dot(va, vb):
	return va[0] * vb[0] + va[1] * vb[1]


def abs(va):
	return (va[0] * va[0] + va[1] * va[1]) ** 0.5


def norm(va):
	return va[0]/abs(va), va[1]/abs(va)


def angle(src, a, b):
	va = (a[0]-src[0], a[1]-src[1])
	vb = (b[0]-src[0], b[1]-src[1])
	return dot(va, vb)/(abs(va) * abs(vb)) >= 0.7


def translate(s, t):
	return s[0] + t[0], s[1] + t[1]


def generate_heuristics(radius_squared):
	rSq = radius_squared
	r = int(rSq ** 0.5)
	d = r * 2 + 1
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

	l_dump = []
	d_dump = []
	dest_dump = []
	all_loc = []

	for x in range(-r, r+1):
		lim = int((rSq - x*x)**0.5)
		for y in range(-lim, lim+1):
			if (x, y) != (0, 0):
				all_loc.append((x, y))

	for dx, dy in dirs:
		dest = (dx*r, dy*r)
		dest_far = (dx*d*2, dy*d*2)
		src = (0, 0)
		my = sorted(all_loc, key=lambda l: distance(l, src), reverse=True)
		my = [translate(a, (r, r)) for a in my if angle(src, dest, a)]
		l_dump.append(my)
		my = sorted(all_loc, key=lambda l: distance(l, dest_far), reverse=True)
		my = [translate(a, (r, r)) for a in my]
		dest_dump.append(my[-5:])
		my = sorted(dirs, key=lambda l: dot(norm((dx, dy)), norm(l)), reverse=True)
		d_dump.append(my[-3:])

	file = open(f"generated/Heuristics{rSq}.java", 'w')
	file.write(f'''package gen5.common.generated;
	
	
public class Heuristics{rSq} {{
''')

	file.write("\n\tpublic static int[][] locationDumpX = {\n")
	for arr in l_dump:
		out = "\t\t{ "
		file.write("")
		for x, _ in arr:
			out += f"{x}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write("\n\tpublic static int[][] locationDumpY = {\n")
	for arr in l_dump:
		out = "\t\t{ "
		file.write("")
		for _, y in arr:
			out += f"{y}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write("\n\tpublic static int[][] directionDumpX = {\n")
	for arr in d_dump:
		out = "\t\t{ "
		file.write("")
		for x, _ in arr:
			out += f"{x}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write("\n\tpublic static int[][] directionDumpY = {\n")
	for arr in d_dump:
		out = "\t\t{ "
		file.write("")
		for _, y in arr:
			out += f"{y}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write("\n\tpublic static int[][] destinationDumpX = {\n")
	for arr in dest_dump:
		out = "\t\t{ "
		file.write("")
		for x, _ in arr:
			out += f"{x}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write("\n\tpublic static int[][] destinationDumpY = {\n")
	for arr in dest_dump:
		out = "\t\t{ "
		file.write("")
		for _, y in arr:
			out += f"{y}, "
		out += "},\n"
		file.write(out)
	file.write("\t};\n")

	file.write('}\n')
	file.close()


generate_heuristics(53)
