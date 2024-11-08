package com.example.cityartwalk
//List UI
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cityartwalk.databinding.FragmentListBinding

class ListFragment : Fragment(), RecordAdapter.OnItemClickListener {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var recordViewModel: RecordViewModel
    private lateinit var adapter: RecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true) //turning on the options menu, gotta show those extra options
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //setting up the adapter and connecting the click listener
        adapter = RecordAdapter()
        adapter.setOnItemClickListener(this)

        //setting up recycler view layout and assigning adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        //linking up the view model to get all the records
        recordViewModel = ViewModelProvider(this)[RecordViewModel::class.java]
        recordViewModel.allRecords.observe(viewLifecycleOwner) { records ->
            records?.let { adapter.setRecords(it) } //updating the adapter when records change
        }

        //click listener for the add button
        binding.buttonAddRecord.setOnClickListener {
            val action = ListFragmentDirections.actionListFragmentToItemFragment(-1)
            findNavController().navigate(action) //navigating to add a new record
        }
    }

    override fun onItemClick(record: Record) {
        //navigating to the item fragment for the clicked record
        val action = ListFragmentDirections.actionListFragmentToItemFragment(record.id)
        findNavController().navigate(action)
    }

    //inflate the menu with the options
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_list, menu) //adding our menu items
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_help -> {
                //navigating to the help fragment when the help option is clicked
                val action = ListFragmentDirections.actionListFragmentToHelpFragment()
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null //cleaning up the binding when the view is destroyed
    }
}
