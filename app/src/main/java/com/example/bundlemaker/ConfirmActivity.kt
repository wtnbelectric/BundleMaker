        val controlSerial = findViewById<TextView>(R.id.confirm_control_serial)
        val registerBtn = findViewById<Button>(R.id.confirm_register_button)
        val modifyBtn = findViewById<Button>(R.id.confirm_modify_button)
        val cancelBtn = findViewById<Button>(R.id.confirm_cancel_button)

        productSerial.text = product?.product_serial ?: ""
        robotSerial.text = product?.robot_serial ?: ""
        controlSerial.text = product?.control_serial ?: ""

        registerBtn.setOnClickListener {
            val result = Intent().apply {
                putExtra("action", "register")
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
        modifyBtn.setOnClickListener {
            val result = Intent().apply {
                putExtra("action", "modify")
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
        cancelBtn.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}

