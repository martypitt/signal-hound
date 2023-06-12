import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'app-sidebar',
  template: `
      <div class="flex grow flex-col gap-y-5 overflow-y-auto border-r border-gray-200 bg-white px-6 pb-4">
          <div class="flex h-16 shrink-0 items-center">
              <img class="h-8 w-auto" src="https://tailwindui.com/img/logos/mark.svg?color=indigo&shade=600"
                   alt="Your Company">
          </div>
          <nav class="flex flex-1 flex-col">
              <ul role="list" class="-mx-2 space-y-1">
                  <li *ngFor="let link of links">
                      <!-- Current: "bg-gray-50 text-indigo-600", Default: "text-gray-700 hover:text-indigo-600 hover:bg-gray-50" -->
                      <a [routerLink]="link.path"
                         class="text-gray-700 hover:text-indigo-600 hover:bg-gray-50 group flex gap-x-3 rounded-md p-2 text-sm leading-6 font-semibold">
                          <tui-svg [src]="link.icon"
                                   class="h-6 w-6 shrink-0 text-gray-400 group-hover:text-indigo-600"></tui-svg>
                          {{link.title}}
                      </a>
                  </li>

              </ul>
          </nav>
      </div>
  `,
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {

  links = [
    {title: 'Feeds', icon: 'tuiIconRss', path: ''},
    {title: 'Settings', icon: 'tuiIconSettings', path: ''}
  ]

}
