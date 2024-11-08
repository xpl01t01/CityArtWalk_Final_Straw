package com.example.cityartwalk
//Item UI
import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.cityartwalk.databinding.FragmentItemBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ItemFragment : Fragment() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

    private var _binding: FragmentItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var recordViewModel: RecordViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var recordId: Int = -1
    private var savedBitmap: Bitmap? = null
    private var currentRecord: Record? = null //cached record object

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            setScaledBitmap(bitmap)
            savedBitmap = bitmap
            Toast.makeText(requireContext(), "photo taken successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "failed to take photo", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val inputStream = requireContext().contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            setScaledBitmap(bitmap)
            savedBitmap = bitmap
            Toast.makeText(requireContext(), "photo selected from gallery", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recordViewModel = ViewModelProvider(
            this,
            SavedStateViewModelFactory(requireActivity().application, this)
        )[RecordViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        //retrieve the passed record id
        arguments?.let {
            recordId = ItemFragmentArgs.fromBundle(it).recordId
        }

        if (recordId != -1) {
            //load existing record
            recordViewModel.getRecordById(recordId).observe(viewLifecycleOwner) { record ->
                currentRecord = record

                //update ui with record data
                binding.titleEditText.setText(record.title)
                binding.addressEditText.setText(record.address)
                binding.selectDateButton.text = record.date

                Log.d(
                    "ItemFragment",
                    "loaded record: id=${record.id}, latitude=${record.latitude}, longitude=${record.longitude}, address=${record.address}"
                )

                //update location display
                if ((record.latitude != 0.0 || record.longitude != 0.0)) {
                    val displayText = if (!record.address.isNullOrEmpty() && record.address != "address not found") {
                        "location: ${record.address}"
                    } else {
                        "location: lat: ${record.latitude}, lng: ${record.longitude}"
                    }
                    binding.locationTextView.text = displayText
                    binding.showMapButton.isEnabled = true
                } else {
                    binding.locationTextView.text = "location: not available"
                    binding.showMapButton.isEnabled = false
                }

                //other ui updates (e.g., image, buttons) if needed
            }
        } else {
            //initialize new record
            currentRecord = Record()
            binding.showMapButton.isEnabled = false
            binding.locationTextView.text = "location: not available"
        }

        binding.selectDateButton.setOnClickListener {
            selectDate()
        }

        binding.saveButton.setOnClickListener {
            saveRecord()
        }

        binding.deleteButton.setOnClickListener {
            deleteRecord()
        }

        binding.getLocationButton.setOnClickListener {
            getGpsLocation()
        }

        binding.showMapButton.setOnClickListener {
            showMap()
        }

        binding.shareRecordButton.setOnClickListener {
            shareRecord()
        }

        binding.takePhotoButton.setOnClickListener {
            takePhoto()
        }

        binding.pickPhotoButton.setOnClickListener {
            pickPhotoFromGallery()
        }

        binding.imageViewPhoto.setOnClickListener {
            zoomImage()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.showMapButton.isEnabled = currentRecord?.latitude != 0.0 && currentRecord?.longitude != 0.0
    }

    private fun setScaledBitmap(bitmap: Bitmap) {
        binding.imageViewPhoto.doOnLayout { measuredView ->
            val scaledBitmap = getScaledBitmap(bitmap, measuredView.width, measuredView.height)
            binding.imageViewPhoto.setImageBitmap(scaledBitmap)
        }
    }

    private fun getScaledBitmap(bitmap: Bitmap, destWidth: Int, destHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, destWidth, destHeight, true)
    }

    private fun selectDate() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)
                binding.selectDateButton.text = formattedDate
                currentRecord?.date = formattedDate //update date in currentRecord
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun saveRecord() {
        val title = binding.titleEditText.text.toString()
        val address = binding.addressEditText.text.toString()
        val date = binding.selectDateButton.text.toString()

        currentRecord?.let { record ->
            record.title = title
            record.address = address
            record.date = date

            lifecycleScope.launch(Dispatchers.IO) {
                if (recordId == -1) {
                    val newId = recordViewModel.insert(record)
                    recordId = newId.toInt()
                    record.id = recordId  //ensure record.id is mutable
                } else {
                    recordViewModel.update(record)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "record saved", Toast.LENGTH_SHORT).show()
                    //navigate back or update ui as needed
                }
            }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): String? {
        return try {
            val filename = "image_${System.currentTimeMillis()}.png"
            requireContext().openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            filename //return the filename
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadImageFromInternalStorage(filename: String): Bitmap? {
        return try {
            val file = requireContext().getFileStreamPath(filename)
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deleteRecord() {
        currentRecord?.let {
            //delete the image file if it exists
            it.imagePath?.let { path ->
                deleteImageFromInternalStorage(path)
            }
            recordViewModel.delete(it)
            findNavController().navigateUp() //navigate back to the listfragment
        } ?: run {
            Toast.makeText(requireContext(), "record not loaded yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteImageFromInternalStorage(filename: String) {
        try {
            requireContext().deleteFile(filename)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getGpsLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val location: Location = task.result

                    currentRecord?.let { record ->
                        record.latitude = location.latitude
                        record.longitude = location.longitude

                        Log.d("ItemFragment", "location obtained: lat=${location.latitude}, lng=${location.longitude}")

                        //reverse geocoding to get address
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())

                        lifecycleScope.launch(Dispatchers.IO) {
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val addressText = if (!addresses.isNullOrEmpty()) {
                                addresses[0].getAddressLine(0)
                            } else {
                                "address not found"
                            }

                            //update the address in the record
                            record.address = addressText

                            Log.d("ItemFragment", "address obtained: $addressText")

                            //save the updated record to the database
                            try {
                                if (recordId == -1) {
                                    //new record: insert into database
                                    val newId = recordViewModel.insert(record)
                                    recordId = newId.toInt()
                                    record.id = recordId
                                    Log.d("ItemFragment", "new record inserted with id: $recordId")
                                } else {
                                    //existing record: update database
                                    recordViewModel.update(record)
                                    Log.d("ItemFragment", "existing record updated with id: $recordId")
                                }
                            } catch (e: Exception) {
                                Log.e("ItemFragment", "error saving record: ${e.message}")
                            }

                            withContext(Dispatchers.Main) {
                                //update the ui
                                val displayText = if (!record.address.isNullOrEmpty() && record.address != "address not found") {
                                    "location: ${record.address}"
                                } else {
                                    "location: lat: ${record.latitude}, lng: ${record.longitude}"
                                }
                                binding.locationTextView.text = displayText
                                binding.showMapButton.isEnabled = true
                                Toast.makeText(requireContext(), "location updated and saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.w("ItemFragment", "failed to get location.")
                    Toast.makeText(requireContext(), "unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUIWithAddress(addressText: String) {
        binding.showMapButton.isEnabled = true
        binding.locationTextView.text = "location: $addressText"
        Toast.makeText(requireContext(), "location updated", Toast.LENGTH_SHORT).show()

        //update the address in the record
        currentRecord?.address = addressText

        //if the record is existing, update it in the database
        if (recordId != -1) {
            lifecycleScope.launch {
                currentRecord?.let { recordViewModel.update(it) }
            }
        }
    }

    private fun showMap() {
        currentRecord?.let {
            if (it.latitude != 0.0 && it.longitude != 0.0) {
                val geoUri = "geo:${it.latitude},${it.longitude}?q=${it.latitude},${it.longitude}(location)"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))

                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "no application available to view map", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "location not available to show on map", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(requireContext(), "record not loaded yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareRecord() {
        val title = currentRecord?.title ?: "no title"
        val address = currentRecord?.address ?: "no address"
        val date = currentRecord?.date ?: "no date"
        val latitude = currentRecord?.latitude
        val longitude = currentRecord?.longitude

        val locationText = if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
            """
        location:
        latitude: $latitude
        longitude: $longitude
        view on map: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude
        """.trimIndent()
        } else {
            "location: not available"
        }

        val shareText = """
        title: $title
        address: $address
        date: $date
        $locationText
    """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "shared record")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "share via"))
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            takePictureLauncher.launch(null)
        }
    }

    private fun pickPhotoFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun zoomImage() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_enlarge_image, null, false)
        val imageView = dialogView.findViewById<ImageView>(R.id.zoomedImageView)
        imageView.setImageDrawable(binding.imageViewPhoto.drawable)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("close") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
