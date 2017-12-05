"use strict";

const Root = Object.freeze({
	CONSTANT: `Constant string value`,

	functionOne: => {
		// TODO: Load value.
		return `Value loaded from somewhere.`;
	},

	functionTwo: argument => {
		let newValue = null;
		switch (argument) {
			case `Option 1`:
				// TODO: Do something.
				newValue = `Option 1`;
				break;
			case `Option 2`:
				// TODO: Do something else.
				newValue = `Option 2`;
				break;
			default:
				console.log(`Unrecognised argument: [${argument}]`);
		}
		return newValue;
	},

	functionSum: (argumentOne, argumentTwo) => {
		return argumentOne + argumentTwo;
	}
});
