radiusSquared = 34

r = int(radiusSquared**0.5)
d = 2*r + 1

jobs = [
	{
		'name': 'Occupied',
		'data_type': 'boolean',
		'value': 'true',
		'ranges': [
			(1, d+1),
			(1, d+1),
		]
	},
	{
		'name': 'Distance',
		'data_type': 'int',
		'value': '100000',
		'ranges': [
			(1, d+1),
			(1, d+1),
		]
	},
]

file = open(f'generated/ArrayFiller{radiusSquared}.java', 'w')

file.write(f'''package gen5.common.bellmanford;


public class ArrayFiller{radiusSquared} implements ArrayFiller {{
''')

for job in jobs:
	out = '\n\tpublic void fill{name}({data_type}[][] arr) {{\n'.format(**job)
	value = job['value']
	for i in range(*job['ranges'][0]):
		for j in range(*job['ranges'][1]):
			out += f'\t\tarr[{i}][{j}] = {value};\n'

	out += '\t}\n'
	file.write(out)

file.write('}\n')
file.close()
